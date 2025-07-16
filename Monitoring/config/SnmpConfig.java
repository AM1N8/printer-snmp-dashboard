package com.snmp.Monitoring.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "snmp")
public class SnmpConfig {

    private String version = "2c";
    private String community = "public";
    private int timeout = 5000;
    private int retries = 3;
    private int port = 1161;

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
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getCommunity() {
        return community;
    }

    public void setCommunity(String community) {
        this.community = community;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}