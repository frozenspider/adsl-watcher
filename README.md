adsl-watcher
==============

Watches the ADSL modem/router status, logging connection details to a database.
Supports only my personal modem and router models (Upvel UR344AN4G+, Tenda D820B),
but extensible framework support is implemented.


Configuration
-------------
You'll need to copy `application.conf.example` as `application.conf`, changing values according to your own setup.

`device.interface` setting controls which modem interface the adsl-watcher should query.
For specific devices:

Device name         | `interface` meaning
------------------- | -------------------
Upvel UR344AN4G+    | Interface index in the dropdown, e.g. 0 for for PVC0
Tenda D820B         | None


Changelog
---------

### SNAPSHOT
* Support for several more modulation types:
  * G.dmt
  * G.lite
  * ADSL2+ Auto

### 1.6
* Renamed project from `router-watcher` to `adsl-watcher`
* Detector for Tenda D820B
* Removed RouterDiscoverer, IP should now be specified explicitly through config

### 1.5
* Fixed improper autoupdate behaviour of timestamp column
* Config format changes
  * Added `router.interface` parameter
  * `period` changed to `period.longterm`
    * Done for planned feature of short-term detailed recording
* Tweaks to Upvel UR344AN4G+ detector
  * It now reuses the same session after initial login if possible, fixing spamming log
    with `2017-10-31 14:15:20 [Informational] WEB: WEB user <admin> login`
  * Support for firmware v1.782

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
