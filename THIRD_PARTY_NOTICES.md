# Third-Party Notices

CPAuctionHouse (**1.0.0-RC1**) bundles the following third-party components. Their use is subject to the respective licenses listed below.

---

## AnvilGUI

| | |
|---|---|
| **Author** | WesJD (Wesley Smith) |
| **License** | [MIT License](https://github.com/WesJD/AnvilGUI/blob/master/LICENSE) |
| **Purpose** | Cross-version anvil text input for GUI sell price and search flows |
| **Homepage** | https://github.com/WesJD/AnvilGUI |

Shaded and relocated to `de.crafterspoint.cpauctionhouse.libs.anvilgui` inside the plugin JAR.

Note: `Wrapper26_R1` (Java 25) is excluded from the shaded artifact for compatibility with Java 21 server runtimes.

---

## SQLite JDBC

| | |
|---|---|
| **Author** | Xerial |
| **License** | [Apache License 2.0](https://github.com/xerial/sqlite-jdbc/blob/master/LICENSE) |
| **Purpose** | Embedded SQLite database driver for default auction storage |
| **Homepage** | https://github.com/xerial/sqlite-jdbc |

Shaded into the plugin JAR (includes native libraries for supported platforms).

---

## MySQL Connector/J

| | |
|---|---|
| **Author** | Oracle |
| **License** | [GPL 2.0 with Universal FOSS Exception](https://www.mysql.com/about/legal/licensing/oem/) |
| **Purpose** | JDBC driver for optional MySQL/MariaDB auction storage |
| **Homepage** | https://github.com/mysql/mysql-connector-j |

Shaded into the plugin JAR (not relocated).

---

## HikariCP

| | |
|---|---|
| **Author** | brettwooldridge |
| **License** | [Apache License 2.0](https://github.com/brettwooldridge/HikariCP/blob/dev/LICENSE) |
| **Purpose** | JDBC connection pooling for MySQL/MariaDB storage |
| **Homepage** | https://github.com/brettwooldridge/HikariCP |

Shaded and relocated to `de.crafterspoint.cpauctionhouse.libs.hikari`.

---

## SLF4J API

| | |
|---|---|
| **Author** | QOS.ch |
| **License** | [MIT License](https://www.slf4j.org/license.html) |
| **Purpose** | Logging API required by HikariCP |
| **Homepage** | https://www.slf4j.org/ |

Shaded into the plugin JAR.

---

## Provided at runtime (not bundled)

| Component | Purpose |
|-----------|---------|
| **Spigot API** | Server plugin API (compile-only) |
| **Vault API** | Economy bridge (soft dependency at runtime) |

---

## CPAuctionHouse itself

CPAuctionHouse is **proprietary** software. See [LICENSE.txt](LICENSE.txt) and [EULA.md](EULA.md). Third-party licenses above apply only to their respective components.
