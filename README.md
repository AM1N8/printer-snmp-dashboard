# SNMP Printer Monitor Setup Guide

## Overview

This guide walks you through setting up an SNMP-based printer monitoring system using Java and snmpsim. The system allows you to read printer information like system description, uptime, contact information, and location using the Simple Network Management Protocol (SNMP).

## Prerequisites

- Java 8 or higher
- Maven or Gradle for dependency management
- Python 3.x (for snmpsim)
- Basic understanding of networking concepts

## Installation

### 1. Install snmpsim

```bash
pip install snmpsim
```

### 2. Add Java Dependencies

Add the following dependency to your `pom.xml` (Maven):

```xml
<dependency>
    <groupId>org.snmp4j</groupId>
    <artifactId>snmp4j</artifactId>
    <version>3.7.7</version>
</dependency>
```

Or for Gradle (`build.gradle`):

```gradle
implementation 'org.snmp4j:snmp4j:3.7.7'
```

## Setting Up the SNMP Simulator

### 1. Create SNMP Record File

Create a file named `printer.snmprec` in your project directory:

```
1.3.6.1.2.1.1.1.0|4|Demo System Description
1.3.6.1.2.1.1.3.0|67|12345
1.3.6.1.2.1.1.4.0|4|admin@company.com
1.3.6.1.2.1.1.5.0|4|Office Printer HP-001
1.3.6.1.2.1.1.6.0|4|Building A, Floor 2, Room 201
```

### 2. Understanding the Record Format

Each line follows the format: `OID|TYPE|VALUE`

- **OID**: Object Identifier (unique identifier for each data point)
- **TYPE**: Data type (4 = OctetString, 67 = Timeticks, 2 = Integer)
- **VALUE**: The actual value to return

### 3. Start the SNMP Simulator

```bash
snmpsim-command-responder --data-dir=. --agent-udpv4-endpoint=127.0.0.1:161
```

**Note**: You may need to run with `sudo` on Linux/Mac if using port 161.

## Java Code Explanation

### Key Components

1. **SNMP4J Library**: Handles SNMP communication
2. **Community Target**: Defines the SNMP agent connection parameters
3. **PDU (Protocol Data Unit)**: Contains the SNMP request
4. **OIDs**: Specific identifiers for printer information

### Important OIDs

| OID | Description | Type |
|-----|-------------|------|
| 1.3.6.1.2.1.1.1.0 | System Description | String |
| 1.3.6.1.2.1.1.3.0 | System Uptime | Timeticks |
| 1.3.6.1.2.1.1.4.0 | System Contact | String |
| 1.3.6.1.2.1.1.5.0 | System Name | String |
| 1.3.6.1.2.1.1.6.0 | System Location | String |

## Running the Application

1. Compile and run the Java application:
```bash
javac -cp "snmp4j-3.7.7.jar" PrinterSnmpReader.java
java -cp ".:snmp4j-3.7.7.jar" PrinterSnmpReader
```

2. Expected output:
```
System Description: Demo System Description
Uptime: 12345
Contact: admin@company.com
Name: Office Printer HP-001
Location: Building A, Floor 2, Room 201
```

## Expanding the System

### 1. Adding Printer-Specific OIDs

Add these printer-specific OIDs to your `.snmprec` file:

```
# Printer Status
1.3.6.1.2.1.25.3.2.1.5.1|2|3
# Pages Printed
1.3.6.1.2.1.43.10.2.1.4.1.1|2|15000
# Toner Level (Black)
1.3.6.1.2.1.43.11.1.1.9.1.1|2|75
# Paper Tray Status
1.3.6.1.2.1.43.8.2.1.10.1.1|2|0
# Error Status
1.3.6.1.2.1.43.18.1.1.8.1.1|2|0
```

### 2. Enhanced Java Code

```java
// Add these constants
private static final String OID_PRINTER_STATUS = "1.3.6.1.2.1.25.3.2.1.5.1";
private static final String OID_PAGES_PRINTED = "1.3.6.1.2.1.43.10.2.1.4.1.1";
private static final String OID_TONER_LEVEL = "1.3.6.1.2.1.43.11.1.1.9.1.1";

// Add these calls in main method
fetchAndPrint(snmp, target, "Printer Status", OID_PRINTER_STATUS);
fetchAndPrint(snmp, target, "Pages Printed", OID_PAGES_PRINTED);
fetchAndPrint(snmp, target, "Toner Level", OID_TONER_LEVEL);
```

### 3. Creating a Printer Status Interpreter

```java
private static String interpretPrinterStatus(int status) {
    switch (status) {
        case 1: return "Other";
        case 2: return "Unknown";
        case 3: return "Idle";
        case 4: return "Printing";
        case 5: return "Warmup";
        default: return "Unknown Status";
    }
}
```

### 4. Adding Error Handling and Logging

```java
import java.util.logging.Logger;
import java.util.logging.Level;

private static final Logger logger = Logger.getLogger(PrinterSnmpReader.class.getName());

private static void fetchAndPrintWithErrorHandling(Snmp snmp, CommunityTarget target, String label, String oidStr) {
    try {
        PDU pdu = new PDU();
        pdu.add(new VariableBinding(new OID(oidStr)));
        pdu.setType(PDU.GET);

        ResponseEvent response = snmp.get(pdu, target);

        if (response != null && response.getResponse() != null) {
            VariableBinding vb = response.getResponse().get(0);
            if (vb.getVariable().getSyntax() == SMIConstants.SYNTAX_NULL) {
                logger.warning(label + ": No such object");
                System.out.println(label + ": No such object");
            } else {
                System.out.println(label + ": " + vb.getVariable());
            }
        } else {
            logger.severe(label + ": No response or timeout");
            System.out.println(label + ": No response or timeout");
        }
    } catch (Exception e) {
        logger.log(Level.SEVERE, "Error fetching " + label, e);
        System.out.println(label + ": Error - " + e.getMessage());
    }
}
```

### 5. Multiple Printer Support

```java
private static final String[] PRINTER_IPS = {
    "127.0.0.1", "192.168.1.100", "192.168.1.101"
};

public static void main(String[] args) throws Exception {
    for (String printerIp : PRINTER_IPS) {
        System.out.println("\n=== Printer: " + printerIp + " ===");
        monitorPrinter(printerIp);
    }
}

private static void monitorPrinter(String printerIp) throws Exception {
    // Move your existing main method logic here
    // Replace PRINTER_IP with printerIp parameter
}
```

### 6. Scheduled Monitoring

```java
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PrinterMonitorService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    public void startMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("\n=== Monitoring Check: " + new Date() + " ===");
                // Your monitoring logic here
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Monitoring error", e);
            }
        }, 0, 5, TimeUnit.MINUTES); // Check every 5 minutes
    }
    
    public void stopMonitoring() {
        scheduler.shutdown();
    }
}
```

## Testing with Real Printers

### 1. Network Printer Setup

1. Ensure the printer has SNMP enabled
2. Configure the community string (usually "public" by default)
3. Replace `127.0.0.1` with the printer's IP address
4. Update the community string in your code

### 2. Security Considerations

- Use SNMPv3 for production environments
- Change default community strings
- Implement proper authentication and encryption
- Restrict SNMP access to authorized networks

### 3. Common Issues and Solutions

| Issue | Solution |
|-------|----------|
| Timeout errors | Increase timeout value or check network connectivity |
| Permission denied | Run with appropriate privileges or use non-privileged ports |
| Wrong community string | Verify the community string with network administrator |
| Firewall blocking | Configure firewall to allow SNMP traffic (UDP 161) |

## Advanced Features

### 1. Web Dashboard

Create a simple web interface using Spring Boot to display printer status in real-time.

### 2. Alert System

Implement email or SMS alerts when printers are low on toner or have errors.

### 3. Database Integration

Store printer data in a database for historical analysis and reporting.

### 4. REST API

Create RESTful endpoints to query printer status programmatically.

## Troubleshooting

1. **Connection Issues**: Verify IP address and port accessibility
2. **Authentication Problems**: Check community string configuration
3. **OID Not Found**: Verify OID exists in your `.snmprec` file
4. **Performance Issues**: Implement connection pooling and caching

## Best Practices

1. Use connection pooling for multiple printers
2. Implement proper error handling and logging
3. Cache frequently accessed data
4. Use configuration files for settings
5. Follow SNMP security best practices
6. Monitor network traffic to avoid overwhelming devices

## Resources

- [SNMP4J Documentation](https://www.snmp4j.org/)
- [snmpsim Documentation](https://snmpsim.readthedocs.io/)
- [RFC 1157 - SNMP Specification](https://tools.ietf.org/html/rfc1157)
- [Printer MIB Documentation](https://www.pwg.org/standards.html)
