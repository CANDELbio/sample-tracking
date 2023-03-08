(ns org.parkerici.sample-tracking.utils.js)

(defn set-body-bg-color
  [color]
  (set! (.. js/document -body -style -backgroundColor) color))