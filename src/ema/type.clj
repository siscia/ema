(ns ema.type
  (:require [clojure.core.typed :as t]))

(t/defalias PreResourceDefinition
  (t/HMap :mandatory {:name String}
          :optional {:key t/Keyword
                     :uri String
                     :authentication t/Keyword
                     :item-entries (t/Seqable t/Keyword)
                     :collection-entries (t/Seqable t/Keyword)}))

(t/defalias EntryMap
  (t/HMap :mandatory {:handler t/Keyword
                      :collections (t/Seqable PreResourceDefinition)}
          :optional {:key t/Keyword
                     :uri String}))

(t/defalias ResourceDefinition
  (t/HMap :mandatory {:key t/Keyword
                      :name String
                      :uri String
                      :handler t/Keyword}
          :optional {:item-entries (t/Seq t/Keyword)
                     :collection-entries (t/Seq t/Keyword)
                     :authentication t/Keyword}))
