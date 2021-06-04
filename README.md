# rds-utlilization

Tool for gathering and reporting Amazon Web Services (AWS) usage of Relational Database Service (RDS) accross the regions over predefined period of time.

## Usage

Before using the tool make sure to setup appropriate AWS related environment variables (AWS\_ACCESS\_KEY\_ID, AWS\_SECRET\_ACCESS\_KEY, AWS\_SESSION\_TOKEN), so RDS and CloudWatch data could accessed via Amazon API. Before running the tool use leiningen to build uberjar:

    $ lein uberjar 
    $ java -jar ./target/uberjar/rds-utlilization-0.1.0-SNAPSHOT-standalone.jar

By default the tools lists all the RDS instances and clusters created on a given account in Ireland(EU), N.Virginia(US) and Tokyo(APAC) regions. 

## Library

There is a number of utility functions available that could be used for:

* listing all events (max from past 14 days - which is a limitation of AWS API) performed on specified RDS
* listing all snapshots for a given RDS
* listing CPU statistics for a given RDS (min, max, avg values from last N days)
* listing RDS instances (PostgreSQL, MySQL, Oracle and SQLServer)
* listing RDS clusters (Aurora)


## License

Copyright © 2019 Krzysztof Kielak

This program and the accompanying materials are made available under the
terms of the MIT License which is available at https://opensource.org/licenses/MIT.

See LICENSE file for more details.
