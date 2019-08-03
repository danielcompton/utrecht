[![Clojars Project](https://img.shields.io/clojars/v/irresponsible/utrecht.svg)](https://clojars.org/irresponsible/utrecht)
[![cljdoc badge](https://cljdoc.org/badge/irresponsible/utrecht)](https://cljdoc.org/d/irresponsible/utrecht)
[![Travis CI](https://travis-ci.org/irresponsible/utrecht.svg?branch=master)](https://travis-ci.org/irresponsible/utrecht)

The irresponsible clojure guild presents...

# utrecht

Just enough rope to wrangle a jdbc.

A modern, minimalist database library with an emphasis on correctness,
stability and performance. We provide a connection pool and a small
library that makes it easy to work with Postgres.


## Features

* Rock-solid HikariCP database pool
* Simple api
* Support for transactions (all isolations) and prepared queries
* Optional interfaces for 'component' and 'codependence

Note that while this should in theory work against any sane RDBMS, we only test against postgres. Patches welcome.

## Usage

```clojure
(ns my.db
 (:require [irresponsible.utrecht :as u]
           [irresponsible.utrecht.pool.hikaricp :refer [hikaricp]])
 (:import  [clojure.lang ExceptionInfo]))

(def pool (hikaricp {:server-name "127.0.0.1" :username "foo" :password "bar"}))
(def bars (u/with-conn [conn pool]
            (u/with-prep conn [q "select * from foo where bar = ?"]
              (u/query conn q ["bar"]))) ; query can also take a sql string
(def quuxs (try ; with-transaction binds a connection like with-conn
              (u/with-transaction :ro :serializable [conn pool]
                (let [sp1 (u/savepoint conn :sp1) ; savepoints!
                      r   (u/query conn "select 'foo' as result")]
                  (u/rollback sp1) ; rolling back to savepoints!
                  (throw (ex-info "throw to rollback the entire transaction" {:result r}))))
              (catch ExceptionInfo e ; yes, your exception is rethrown
                (:result (ex-data e)))))

;; During shutdown you'll want to close the pool
(.close pool)
```

Options are documented in the [hikari-cp](https://github.com/tomekw/hikari-cp) README.

If you use pgjdbc-ng, it unhelpfully chooses different property names from the postgres adapter.

## Recommendations

Goes nicely with:

* A recent version of postgres
* [mpg](https://github.com/mpg-project/mpg)
* [codependence](https://github.com/irresponsible/codependence)

## Hacking

### Running Tests

Testing requires a configured **PostgreSQL** database to perform tests against,
and requires expressing that configuration by use of *environment variables*.

Very basic usage is as simple as

```shell
UTRECHT_TEST_DB="utrecht_test" boot test
```

Which will execute tests against an assumed **PostgreSQL** server running locally on the default ports
without any specific authentication requirements.

If these defaults are not to your liking, the following *environment variables* are settable:

```shell
UTRECHT_TEST_HOST="127.0.0.1"
UTRECHT_TEST_CONN_TIMEOUT="5000" # milliseconds
UTRECHT_TEST_USER=""
UTRECHT_TEST_PASS=""
UTRECHT_TEST_PORT="5432"
```

## Changelog

0.3.0 - Unreleased
 * Moved savepoints to using the proper jdbc APIs rather than executing sql directly
   * Calling `savepoint` now returns a Savepoint object
   * You must pass this savepoint into `rollback`
 * Run tests against pgjdbc-ng as well as postgres jdbc
 * Removed dependence on postgres driver. A user should bring their own
 * We're now compatible back to Java 6 in theory. You should still use Java 8 like we test against.

## Copyright and License

Copyright (c) 2016 James Laver

MIT LICENSE

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
