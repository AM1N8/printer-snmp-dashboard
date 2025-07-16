# Printer Monitoring Dashboard

This project provides a web-based dashboard for monitoring network printers using SNMP (Simple Network Management Protocol). It features a Spring Boot backend for data collection and persistence, and a pure HTML/CSS/JavaScript frontend for visualization. The system is designed to track printer status, toner levels, paper levels, total pages printed, and error messages.

## Table of Contents

- [Features](#features)
- [Prerequisites](#prerequisites)
- [Setup Instructions](#setup-instructions)
  - [1. Database Setup (PostgreSQL)](#1-database-setup-postgresql)
  - [2. SNMP Simulator Setup (snmpsim)](#2-snmp-simulator-setup-snmpsim)
  - [3. Backend Setup (Spring Boot)](#3-backend-setup-spring-boot)
  - [4. Frontend Usage (Dashboard)](#4-frontend-usage-dashboard)
- [Application Architecture & Components](#application-architecture--components)
- [Troubleshooting](#troubleshooting)
- [Future Enhancements](#future-enhancements)

## Features

- **Real-time Monitoring**: Displays current status (Online, Offline, Idle, Printing, Error, Warmup), toner levels, paper levels, total pages printed, and error messages
- **Dashboard Statistics**: Overview of total, online, offline, error, low toner, and low paper printers
- **Add/Delete Printers**: Manage monitored printers via the web interface
- **Individual & Global Refresh**: Manually trigger SNMP polls for specific printers or the entire fleet
- **Scheduled Polling**: Backend automatically updates printer statuses at a configurable interval
- **Themed UI**: Clean and responsive user interface with a white and red color scheme
- **Simulated Testing**: Easy integration with snmpsim for development and testing without physical printers

## Prerequisites

Before you begin, ensure you have the following installed:

- **Java Development Kit (JDK) 17 or higher**: [Download JDK](https://www.oracle.com/java/technologies/downloads/)
- **Apache Maven 3.6.0 or higher**: [Download Maven](https://maven.apache.org/download.cgi)
- **PostgreSQL Database**: [Download PostgreSQL](https://www.postgresql.org/download/)
- **pgAdmin** (recommended for database management): Usually bundled with PostgreSQL installer
- **Python 3.x**: [Download Python](https://www.python.org/downloads/)
- **pip** (Python package installer): Usually comes with Python
- **snmpsim Python package**: Install via pip: `pip install snmpsim`

## Setup Instructions

Follow these steps to get the Printer Monitoring Dashboard up and running.

### 1. Database Setup (PostgreSQL)

**Create a PostgreSQL Database:**

1. Open pgAdmin
2. Connect to your PostgreSQL server
3. Right-click on "Databases" → "Create" → "Database..."
4. Name the database `printerdb`
5. Click "Save"

> **Note**: No need to create tables manually; Spring Boot (Hibernate) will do this automatically.

### 2. SNMP Simulator Setup (snmpsim)

We'll use snmpsim to simulate multiple printers on a single port using virtual IP addresses.

#### A. Install snmpsim

If you haven't already, install snmpsim:

```bash
pip install snmpsim
```

#### B. Configure Virtual Loopback IP Addresses (Windows)

To simulate multiple printers on the same port, we'll add alias IP addresses to your loopback adapter.

1. **Open Command Prompt as Administrator**:
   - Search for `cmd` in the Start menu, right-click, and select "Run as administrator"

2. **Identify your Loopback Adapter**:
   - Run `netsh interface ipv4 show addresses`
   - Look for an adapter named "Loopback Pseudo-Interface 1" or similar
   - Note its exact name or Idx (Index)

3. **Add IP Aliases**:
   Execute the following commands, replacing `<Loopback Adapter Name>` with the actual name:

   ```cmd
   netsh interface ip add address "<Loopback Adapter Name>" 127.0.0.2 255.0.0.0
   netsh interface ip add address "<Loopback Adapter Name>" 127.0.0.3 255.0.0.0
   netsh interface ip add address "<Loopback Adapter Name>" 127.0.0.4 255.0.0.0
   ```

4. **Verify IP Aliases**:
   - Run `ipconfig /all` and check under your loopback adapter to confirm the new IP addresses are listed

#### C. Prepare SNMP Data Files (.snmprec)

Create a main directory (e.g., `C:\snmpsim_data`) and inside it, create subdirectories for each IP address. Place a unique `.snmprec` file in each subdirectory.

**Directory Structure Example:**

```
C:\snmpsim_data\
├── 127.0.0.1\
│   └── printer1.snmprec
├── 127.0.0.2\
│   └── printer2.snmprec
├── 127.0.0.3\
│   └── printer3.snmprec
└── 127.0.0.4\
    └── printer4.snmprec
```

**Example `printer1.snmprec` (for `C:\snmpsim_data\127.0.0.1\printer1.snmprec`):**

```
# printer1.snmprec - Data for 127.0.0.1
1.3.6.1.2.1.1.5.0|4|Printer-A (127.0.0.1)
1.3.6.1.2.1.1.6.0|4|Main Office - Floor 1
1.3.6.1.2.1.25.3.2.1.3.1|4|Acme LaserPro 5000
1.3.6.1.2.1.25.3.5.1.1.1|2|3
1.3.6.1.2.1.43.10.2.1.4.1.1|65|54321
1.3.6.1.2.1.43.11.1.1.9.1.1|2|85
1.3.6.1.2.1.43.8.2.1.10.1.1|2|70
1.3.6.1.2.1.43.18.1.1.8.1.1|4|No current errors
1.3.6.1.2.1.1.3.0|67|123456
1.3.6.1.2.1.1.1.0|4|Simulated Printer A - System Description
1.3.6.1.2.1.43.5.1.1.17.1|4|SIM-A-001
```

Create similar distinct `.snmprec` files for `printer2.snmprec`, `printer3.snmprec`, and `printer4.snmprec` in their respective IP subdirectories. Ensure you change the printer name, location, toner/paper levels, and error messages in each file so they are unique.

#### D. Run the snmpsim Instance

1. Open a NEW terminal or command prompt window
2. Navigate to the parent snmpsim_data directory: `cd C:\snmpsim_data`
3. Run the snmpsim-command-responder command:

```bash
snmpsim-command-responder --data-dir=. --agent-udpv4-endpoint=127.0.0.1:1161 --community=public --logging-level=debug
```

**Command Options:**
- `--data-dir=.`: Tells snmpsim to look for data files in the current directory and its subdirectories
- `--agent-udpv4-endpoint=127.0.0.1:1161`: snmpsim will listen on this single IP and port
- `--community=public`: Matches the default community in your SnmpConfig.java
- `--logging-level=debug`: Provides detailed output for debugging

> **Important**: Keep this terminal window open and snmpsim running.

### 3. Backend Setup (Spring Boot)

#### A. Configure application.properties

Ensure your `src/main/resources/application.properties` file has the correct database and server port configurations:

```properties
# PostgreSQL Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/printerdb
spring.datasource.username=postgres
spring.datasource.password=your_postgres_password # <--- REPLACE WITH YOUR PASSWORD
spring.datasource.driver-class-name=org.postgresql.Driver

# JPA/Hibernate Configuration
spring.jpa.hibernate.ddl-auto=update # Creates/updates tables automatically
spring.jpa.show-sql=true # Show SQL queries in logs
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Server Port (Frontend will access this)
server.port=8081

# SNMP Service Logging Level (for debugging)
logging.level.com.snmp.Monitoring.service=DEBUG
```

#### B. Configure SnmpConfig.java

Ensure your `src/main/java/com/snmp/Monitoring/config/SnmpConfig.java` is configured to use port 1161 and community public:

```java
package com.snmp.Monitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "snmp")
public class SnmpConfig {

    private String version = "2c";
    private String community = "public"; // Make sure this matches snmpsim
    private int timeout = 5000;
    private int retries = 3;
    private int port = 1161; // Make sure this matches snmpsim

    // SNMP OIDs for printer information
    public static final String PRINTER_MODEL_OID = "1.3.6.1.2.1.25.3.2.1.3.1";
    public static final String PRINTER_STATUS_OID = "1.3.6.1.2.1.25.3.5.1.1.1";
    public static final String PRINTER_PAGES_PRINTED_OID = "1.3.6.1.2.1.43.10.2.1.4.1.1";
    public static final String PRINTER_TONER_LEVEL_OID = "1.3.6.1.2.1.43.11.1.1.9.1.1";
    public static final String PRINTER_PAPER_LEVEL_OID = "1.3.6.1.2.1.43.8.2.1.10.1.1";
    public static final String PRINTER_ERROR_OID = "1.3.6.1.2.1.43.18.1.1.8.1.1";
    public static final String SYSTEM_NAME_OID = "1.3.6.1.2.1.1.5.0";
    public static final String SYSTEM_LOCATION_OID = "1.3.6.1.2.1.1.6.0";

    // Getters and Setters
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }
    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    public int getRetries() { return retries; }
    public void setRetries(int retries) { this.retries = retries; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
}
```

#### C. Build and Run

1. Open a NEW terminal or command prompt window
2. Navigate to the root directory of your Spring Boot project (where `pom.xml` is located)
3. Clean and build the project:
   ```bash
   mvn clean install
   ```
4. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```

> **Important**: Keep this terminal window open.

### 4. Frontend Usage (Dashboard)

1. Open your web browser and go to: `http://localhost:8081/index.html`
   - If you changed `server.port` in application.properties, use that port instead of 8081

2. **Add Printers**:
   - Click the "Add New Printer" button
   - Enter the IP addresses you configured in Part 2.B (e.g., 127.0.0.1, 127.0.0.2, 127.0.0.3, 127.0.0.4)
   - Give each a unique name and location
   - Click "Add Printer"

3. **Observe**: After adding, and once the scheduled update runs (default 60 seconds, configurable in PrinterService), you will see the printers appear in the table with their simulated SNMP data.

## Application Architecture & Components

### Backend (Spring Boot)

- **`PrinterMonitorApplication.java`**: The main Spring Boot entry point

- **`model/Printer.java`**: JPA Entity representing a printer, mapped to the printers table. Stores current status, levels, etc.

- **`model/PrinterStatus.java`**: JPA Entity for historical printer status snapshots, mapped to the printer_status table. Records status, levels, and a timestamp.

- **`repository/PrinterRepository.java`**: Spring Data JPA repository for Printer entities. Provides methods for CRUD operations and custom queries (e.g., findByStatus, countOnlinePrinters).

- **`repository/PrinterStatusRepository.java`**: Spring Data JPA repository for PrinterStatus entities, allowing retrieval of historical data.

- **`service/PrinterService.java`**: Contains the core business logic for managing printers. Uses PrinterRepository to interact with the database. Contains a `@Scheduled` method that periodically fetches all printers, uses SnmpService to update their status via SNMP, and saves the updated status back to the DB.

- **`service/SnmpService.java`**: Handles all SNMP communication using the snmp4j library. Provides methods to create SNMP sessions, target devices, and fetch specific OID values. Includes logic to interpret raw SNMP status codes into human-readable strings.

- **`controller/PrinterController.java`**: REST Controller that exposes API endpoints for the frontend. Handles HTTP requests for getting all printers, adding new printers, refreshing printer status, deleting printers, and fetching dashboard statistics.

- **`config/SnmpConfig.java`**: Configuration class that holds SNMP-related settings like default community string, timeout, retries, and a list of OIDs to be queried.

### Frontend (HTML/CSS/JavaScript)

- **`index.html`**: The main dashboard page
  - **HTML Structure**: Defines the layout, stat cards, printer table, and modals for adding printers
  - **CSS**: Styles the dashboard for a clean, modern look with a white and red theme
  - **JavaScript**: Fetches printer data and statistics from the Spring Boot API, dynamically renders the printer table, handles user interactions, and manages loading states

## Troubleshooting

### "Failed to load printers" / CORS Errors

- Ensure your Spring Boot application is running on `http://localhost:8081`
- Verify that `API_BASE_URL` in index.html matches your Spring Boot port
- Confirm `@CrossOrigin(origins = "*")` is present on your PrinterController

### "Cannot connect to printer at 127.0.0.X"

- **Is snmpsim running?** Ensure the snmpsim-command-responder instance is active
- **Correct Port?** Verify snmpsim is listening on 127.0.0.1:1161
- **Correct Community?** Ensure snmpsim is running with `--community=public`
- **Missing OID?** The `isPrinterReachable` method queries `1.3.6.1.2.1.1.3.0` (sysUpTime.0). Make sure this OID is present in ALL your snmprec files
- **Firewall?** Temporarily disable your firewall to test UDP port 1161

### snmpsim Issues

- **Temp file lock**: If snmpsim fails to start with a .dbm file lock error, delete the `C:\Users\YourUser\AppData\Local\Temp\snmpsim` folder and restart snmpsim
- **Not finding .snmprec files**: Ensure the `--data-dir` argument points to the correct parent directory where your IP-named subdirectories reside

## Future Enhancements

- **Data Visualization**: Integrate a charting library (like Chart.js) to display historical trends for toner levels, pages printed, and status distribution
- **Printer Details Page/Modal**: Create a dedicated view for each printer showing more detailed information and graphs
- **User Authentication & Authorization**: Implement Spring Security to protect API endpoints and dashboard access
- **Advanced Status Interpretation**: Expand SnmpService to interpret more granular printer error and status OIDs
- **Alerting System**: Send email or other notifications for critical events (offline, very low toner/paper, specific errors)
- **Search, Filter, Sort**: Add functionality to the printer table for easier navigation
- **Dockerization**: Containerize the Spring Boot application and PostgreSQL for easier deployment
- **Refactor SnmpService for Multi-Port**: Modify SnmpService to dynamically use the printer's specific port if needed
