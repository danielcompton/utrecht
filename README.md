The irresponsible clojure guild presents...

# utrecht

Modern postgres made easy

## What is it?

Just enough of a shim to pull together database pooling
([HikariCP](https://github.com/brettwooldridge/hikaricp/), conversion
between clojure/java and postgres datatypes
([mpg](https://github.com/ShaneKilKelly/mpg/)) and to make it easy to
work with transactions correctly.

## Platform Support

This module requires JDK 8. Please upgrade to JDK 8 to improve
security and performance of your applications.

This module depends on the latest version of the postgres driver at
time of release. You should be able to use it with any non-ancient
version of postgres.

Note that we have explicitly provided full support for prepared
statements. Prepared statements allows the parser and planner to run
once ahead of time and swap in values as required. They're useful if
you wish to execute one particular query many times in
succession. With that, there is a caveat. Some optimisations are not
performed against prepared queries because the actual values required
are needed to perform them. That will include partial indexes where
you don't provide an explicit value for that column.

## Usage

```
(ns my.db
 (:require [irresponsible.utrecht :as u]))

(u/setup!) ;; install the inflation/deflation hooks
(def pool (u/make-pool hikari-pool-opts))
;; Several ways of doing the same thing
(def bars (u/with-conn [conn pool]
            (u/with-prep [q "select * from foo where bar = ?"]
              (u/query conn q ["bar"])))
(def bazs (u/with-conn [conn pool] ; execute unprepared
             (u/run conn "SELECT * FROM foo WHERE bar = ?" ["baz"])))
;; just execute some sql against a pool
(def quuxs (u/q pool "SELECT * FROM foo WHERE bar = ?" ["quux"]))

;; 

;; During shutdown you'll want to close the pool
(.close pool)

```

## Copyright and License

Copyright (c) 2016 James Laver

MIT LICENSE

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
