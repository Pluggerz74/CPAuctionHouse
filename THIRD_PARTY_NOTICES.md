# Third-Party Notices

CPAuctionHouse includes the following third-party components. Their use is subject to the respective licenses.

## AnvilGUI

- **Author:** WesJD (Wesley Smith)
- **License:** [MIT License](https://github.com/WesJD/AnvilGUI/blob/master/LICENSE)
- **Purpose:** Cross-version anvil text input for the auction sell price GUI
- **Homepage:** https://github.com/WesJD/AnvilGUI

The library is shaded and relocated to `de.crafterspoint.cpauctionhouse.libs.anvilgui` inside the plugin JAR.

## SQLite JDBC

- **Author:** Xerial
- **License:** [Apache License 2.0](https://github.com/xerial/sqlite-jdbc/blob/master/LICENSE)
- **Purpose:** Embedded SQLite database driver for auction storage
- **Homepage:** https://github.com/xerial/sqlite-jdbc

The driver is shaded into the plugin JAR.

## MySQL Connector/J

- **Author:** Oracle
- **License:** [GPL 2.0 with Universal FOSS Exception](https://www.mysql.com/about/legal/licensing/oem/)
- **Purpose:** JDBC driver for optional MySQL/MariaDB auction storage
- **Homepage:** https://github.com/mysql/mysql-connector-j

The driver is shaded into the plugin JAR (not relocated).

## HikariCP

- **Author:** brettwooldridge
- **License:** [Apache License 2.0](https://github.com/brettwooldridge/HikariCP/blob/dev/LICENSE)
- **Purpose:** JDBC connection pooling for MySQL/MariaDB storage
- **Homepage:** https://github.com/brettwooldridge/HikariCP

Shaded and relocated to `de.crafterspoint.cpauctionhouse.libs.hikari`.

## SLF4J API

- **Author:** QOS.ch
- **License:** [MIT License](https://www.slf4j.org/license.html)
- **Purpose:** Logging API required by HikariCP
- **Homepage:** https://www.slf4j.org/

Shaded into the plugin JAR.
