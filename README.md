The irresponsible clojure guild presents...

# utrecht

Just enough rope to wrangle a jdbc.

A modern, minimalist database library with an emphasis on correctness,
stability and performance. We provide a connection pool and a small
library that make it easy to work with Postgres.

[![Clojars Project](https://img.shields.io/clojars/v/irresponsible/utrecht.svg)](https://clojars.org/irresponsible/utrecht)

[![Travis CI](https://travis-ci.org/irresponsible/utrecht.svg?branch=master)](https://travis-ci.org/irresponsible/utrecht)

## Features

* HikariCP database pool
* Simple api
* Support for transactions (all isolations) and prepared queries
* Optional 'component' interface

## Usage

This module requires JDK 8. Please upgrade to JDK 8 to improve
the security and performance of your applications.

```clojure
(ns my.db
 (:require [irresponsible.utrecht :as u])
 (:import  [clojure.lang ExceptionInfo]))

(def pool (u/make-pool hikari-pool-opts))
(def bars (u/with-conn [conn pool]
            (u/with-prep [q "select * from foo where bar = ?"]
              (u/query conn q ["bar"]))) ; query can also take a sql string
(def quuxs (try ; with-transaction binds a connection like with-conn
              (u/with-transaction :ro :serializable [conn pool]
                (u/savepoint conn :sp1) ; savepoints!
                (let [r (u/query conn "select 'foo' as result")]
                  (u/rollback :sp1) ; rolling back to savepoints!
                  (throw (ex-info "throw to rollback the entire transaction" {:result r}))))
              (catch ExceptionInfo e ; yes, your exception is rethrown
                (:result (ex-data e)))))

;; During shutdown you'll want to close the pool
(.close pool)
```

## Recommendations

We highly recommend using this module in conjunction with a recent
postgres and [mpg](https://github.com/ShaneKilkelly/mpg) which
provides transparent conversion between pg and clojure data types.

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

## Copyright and License

Copyright (c) 2016 James Laver

MIT LICENSE

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
