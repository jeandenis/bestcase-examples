(ns bestcase.examples.three-page-app.core
  (:use [bestcase.core]
        [bestcase.store.memory]
        [bestcase.util.ring]
        [compojure.core]
        [ring.middleware.params]
        [ring.middleware.session]
        [hiccup.core]
	[hiccup.page])
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.util.response :as resp]))

;; This is a trivial three-page webapp that shows you how to use
;; multi-page a/b tests using bestcase.
;;
;; * The first page ("/") has some text the background-color of which
;;   is determined by :color-test (red, green, blue).
;;
;; * The second page ("/second-page") contains two tests.  The first test
;;   is the same :color-test as the first page.  This is done to show
;;   the user how a test can across multiple pages and show that user
;;   a consistent experience.  The second test is called :text-test
;;   and is an example of how one test can affect the same page in
;;   different sections by having the test return a map as opposed to
;;   html/hiccup
;;
;; * The second page also contains a button to do a "conversion".
;;
;; * The third page ("/third-page") prompts the user to access the
;;   bestcase dashboard to see results.
;;
;; * On the first two pages, the user can also force a certain alternative
;;   using params of the form: &test-name=alternative-name (no leading ":"
;;   necessary).  The second page highlights this.
;;
;; Note: You may want to access the tests from a number of different
;;       browsers/users in order for the result dashboard to be
;;       meaningful.

;; routes
;; ------

(declare get-first-page)
(declare get-second-page)
(declare post-third-page)
(declare get-third-page)

(defroutes app-routes
  (GET "/" [] (get-first-page))
  (GET "/second-page" [] (get-second-page))
  (POST "/third-page" [] (post-third-page))
  (GET "/third-page" [] (get-third-page))

  ;; add the bestcase dashboard, styled with bootstrap
  (dashboard-routes "/bestcase" {:css ["/css/bootstrap.min.css"
                                       "/css/dashboard.css"]})
  
  (route/resources "/")
  (route/not-found "Page Not Found"))
  

(def app
  (do
    ;; before adding middleware, let's tell bestcase to use a memory-store
    (set-config! {:store (create-memory-store)})
    (-> (handler/site app-routes)
        ;; turn on easy-testing (only use this for dev)
        ((identity-middleware-wrapper
          default-identity-fn {:easy-testing true}))
        (wrap-session {:cookie-attrs {:max-age (* 60 60 24 30)}})
        (wrap-params))))

(defmacro head
  [title]
  `[:head
   (include-css "/css/bootstrap.min.css")
    (include-css "/css/two-page-app.css")
    [:title ~title]])

(defmacro body
  [page title & body]
  `[:body
    [:div.container
     [:div.row
      [:div.span8
       [:h1 ~title]
       [:div.navbar
        [:div.navbar-inner
         [:ul.nav
          [(if (= ~page :first-page) :li.active :li)
           [:a {:href "/"} "First Page"]]
          [:li.divider-vertical]
          [(if (= ~page :second-page) :li.active :li)
           [:a {:href "/second-page"} "Second Page"]]
          [:li.divider-vertical]
          [(if (= ~page :third-page) :li.active :li)
           [:a {:href "/third-page"} "Third Page"]]
          [:li.divider-vertical]
          [:li [:a {:href "/bestcase"} "Bestcase Dashboard"]]
          ]]]
       ~@body]]]])

(defn get-first-page
  []
  (html5
   (head "First Page")
   (body
    :first-page "First Page"
    [:div
     [:h3 "Div Background Color"]
     [:div "The background color of the following div is set through the "
      ":color-test a/b test.  You are assigned a random color the first "
      "time you load this page (red, green, or blue) and that color sticks "
      "with you for the other pages in this example webapp."]
     [:br]
     ;; the :color-test a/b test that returns a div w/ the proper css class
     [(alt :color-test
           :red :div.red
           :blue :div.blue
           :green :div.green)
      "The color of this div's background isn't the same for all visitors!"]]
    [:hr]
    [:div [:a.btn {:href "/second-page"} "Continue The Showcase"]])))

(defn get-second-page
  []
  ;; the :text-test a/b test that returns a map. this map is used later
  ;; to determine copy in two separate divs
  (let [t (alt :text-test
               :friendly {:greeting "Hello there friend."
                          :farewell "Take care."}
               :formal {:greeting "Good day sir/madame."
                        :farewell "Goodbye."}
               :robot {:greeting "110101001. (hello in robot)"
                       :farewell "0. (goodbye in robot)"})]
    (html5
     (head "Second Page")
     (body
      :second-page "Second Page"
      [:div
       [:h3 "The Same Div Background Color As Last Page"]
       [:div.highlight (:greeting t)]
       [:br]
       [:div "See, the color sticks with you across the website."]
       [:br]
       ;; the first :color-test a/b test again, note how the color is the
       ;; same as on the first page.
       ;;
       ;; normally, you would not repeat the same code in two different
       ;; place, as here. instead, you would define a function that
       ;; that encapsulates the choices and is used in different views.
       ;;
       ;; For example:
       ;;
       ;; (defn get-div-color []
       ;;   (alt :color-test
       ;;        :red :div.red
       ;;        :green :div.green
       ;;        :blue :div.blue))
       [(alt :color-test
             :red :div.red
             :blue :div.blue
             :green :div.green)
        "Here I am again!"]]
      [:div
       [:h3 "Tests Across Many Elements On Page"]
       [:div
        "The greeting above, as well as the farewell that follows "
        "this paragraph, are determined by the same test so that they "
        "always match."]
       [:br]
       [:div.highlight (:farewell t)]
       [:br]
       [:div
        "You can see all of the different alternatives by "
        "using the query string."]
       [:br]
       [:div [:strong "Colors:"]]
       [:ul
        [:li [:a {:href "/second-page?color-test=red"} "red"]
         " = /second-page?color-test=red"]
        [:li [:a {:href "/second-page?color-test=green"} "green"]
         " = /second-page?color-test=green"]
        [:li [:a {:href "/second-page?color-test=blue"} "blue"]
         " = /second-page?color-test=blue"]]
       [:div [:strong "Greeting & Farewell:"]]
       [:ul
        [:li [:a {:href "/second-page?text-test=friendly"} "friendly"]
         " = /second-page?text-test=friendly"]
        [:li [:a {:href "/second-page?text-test=formal"} "formal"]
         " = /second-page?text-test=formal"]
        [:li [:a {:href "/second-page?text-test=robot"} "robot"]
         " = /second-page?text-test=robot"]]]
      [:div
       [:h3 "Conversions"]
       [:div
        "You can see how conversions (single-participation) work by "
        "clicking on the button hereunder."]
       [:br]
       [:form {:method "post" :action "/third-page"}
        [:button.btn "Conversion!"]]]
      [:br][:br][:br][:br]))))

(defn post-third-page
  []
  (score :color-test :conversion)
  (score :text-test :conversion)
  (resp/redirect "/third-page"))

(defn get-third-page
  []
  (html5
   (head "Third Page")
   (body
    :third-page "Third Page"
    [:div
     [:h3 "Dashboard"]
     [:div "Later, go to the "
      [:a {:href "/bestcase"} "bestcase dashboard"]
      " to see results. That's all we've got."]
     [:br][:br][:br][:br]])))
