# banking-api

Sample Banking API application exercise. Exposing six endpoint to manage an account

## Endpoints

* POST /account - create an account
* GET /account/:id - get account information
* POST /account/:id/deposit - deposit money into account
* POST /account/:id/withdraw - withdraw money from account
* POST /account/:id/send - transfer money from one account to another
* GET /account/:id/audit - see audit with actions in account  

## Swagger

The application exposes Swagger to document and test the API.

Once you have application running go to http://localhost:3000/index.htm

## CURL

You can send CURL requests for endpoints e.g.

`curl -X 'GET' 'http://localhost:3000/account/1' -H 'accept: application/json'`

## Usage

### Testing

#### Single test run

`lein kaocha`

#### Auto-testing

`lein tdd`

### Local with repl

Launch Clojure REPL.

`lein repl`

Start application.

`(start)`

### Run jar

    $ java -jar banking-api-0.1.0-standalone.jar [args]

## License

Copyright © 2023 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
