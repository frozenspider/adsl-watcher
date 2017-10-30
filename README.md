router-watcher
==============

Watches the router status, logging connection details to a database.
Supports only my personal router model (Upvel UR344AN4G+), but extensible framework support is implemented.

Changelog
---------
### 1.5
* Fixed improper autoupdate behaviour of timestamp column
* Implemented frequent data recording, stored temporarily for recent data only
** This allows gathering detailed statistics for the past day or so (configurable),
with excessive data being combed out gradually as the time passes

### 1.4
* Updated to sbt 1.0.2
* Updated dependencies
* Replaced scalaj-http with fs-web-utils
* Slight code improvements
* Integrated Flyway for DB migrations
* Added parsing and persisting of upstream SNR/attenuation/data rate

### 1.3
* Updated to sbt 0.13.9
* HTML parsing extracted to separate library

### 1.2
* Detection framework refactoring
* Content fetching errors are now caught and logged in DB

### 1.1
* Catching socket timeout exceptions
* Added example application.conf
