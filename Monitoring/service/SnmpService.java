package com.snmp.Monitoring.service;

import com.snmp.Monitoring.config.SnmpConfig;
import com.snmp.Monitoring.model.Printer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.*;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.mp.SnmpConstants;
import org.snmp4j.smi.*;
import org.snmp4j.transport.DefaultUdpTransportMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
public class SnmpService {

    private static final Logger logger = LoggerFactory.getLogger(SnmpService.class);

    @Autowired
    private SnmpConfig snmpConfig;

    public CompletableFuture<Printer> updatePrinterStatus(Printer printer) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Snmp snmp = createSnmpSession();
                CommunityTarget target = createTarget(printer.getIpAddress());

                // Update printer information
                updatePrinterInfo(snmp, target, printer);

                snmp.close();
                printer.setLastChecked(LocalDateTime.now());
                return printer;

            } catch (Exception e) {
                logger.error("Error updating printer status for {}: {}", printer.getIpAddress(), e.getMessage());
                printer.setStatus("ERROR");
                printer.setErrorMessage(e.getMessage());
                printer.setLastChecked(LocalDateTime.now());
                return printer;
            }
        });
    }

    private Snmp createSnmpSession() throws IOException {
        TransportMapping transport = new DefaultUdpTransportMapping();
        Snmp snmp = new Snmp(transport);
        transport.listen();
        return snmp;
    }

    private CommunityTarget createTarget(String ipAddress) {
        CommunityTarget target = new CommunityTarget();
        target.setCommunity(new OctetString(snmpConfig.getCommunity()));
        target.setAddress(GenericAddress.parse("udp:" + ipAddress + "/" + snmpConfig.getPort()));
        target.setRetries(snmpConfig.getRetries());
        target.setTimeout(snmpConfig.getTimeout());
        target.setVersion(SnmpConstants.version2c);
        return target;
    }

    private void updatePrinterInfo(Snmp snmp, CommunityTarget target, Printer printer) throws IOException {
        // Check if printer is reachable
        if (!isPrinterReachable(snmp, target)) {
            printer.setStatus("OFFLINE");
            return;
        }

        // Get printer model
        String model = getSnmpValue(snmp, target, SnmpConfig.PRINTER_MODEL_OID);
        if (model != null) {
            printer.setModel(model);
        }

        // Get system name (printer name)
        String systemName = getSnmpValue(snmp, target, SnmpConfig.SYSTEM_NAME_OID);
        if (systemName != null && !systemName.isEmpty()) {
            printer.setName(systemName);
        }

        // Get system location
        String location = getSnmpValue(snmp, target, SnmpConfig.SYSTEM_LOCATION_OID);
        if (location != null) {
            printer.setLocation(location);
        }

        // Get printer status
        String status = getSnmpValue(snmp, target, SnmpConfig.PRINTER_STATUS_OID);
        printer.setStatus(interpretPrinterStatus(status));

        // Get pages printed
        String pagesPrinted = getSnmpValue(snmp, target, SnmpConfig.PRINTER_PAGES_PRINTED_OID);
        if (pagesPrinted != null) {
            try {
                printer.setTotalPagesPrinted(Integer.parseInt(pagesPrinted));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse pages printed: {}", pagesPrinted);
            }
        }

        // Get toner level
        String tonerLevel = getSnmpValue(snmp, target, SnmpConfig.PRINTER_TONER_LEVEL_OID);
        if (tonerLevel != null) {
            try {
                printer.setTonerLevel(Integer.parseInt(tonerLevel));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse toner level: {}", tonerLevel);
            }
        }

        // Get paper level
        String paperLevel = getSnmpValue(snmp, target, SnmpConfig.PRINTER_PAPER_LEVEL_OID);
        if (paperLevel != null) {
            try {
                printer.setPaperLevel(Integer.parseInt(paperLevel));
            } catch (NumberFormatException e) {
                logger.warn("Could not parse paper level: {}", paperLevel);
            }
        }

        // Get error message
        String errorMessage = getSnmpValue(snmp, target, SnmpConfig.PRINTER_ERROR_OID);
        if (errorMessage != null && !errorMessage.isEmpty()) {
            printer.setErrorMessage(errorMessage);
        }
    }

    private boolean isPrinterReachable(Snmp snmp, CommunityTarget target) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(SnmpConfig.SYSTEM_NAME_OID)));
            pdu.setType(PDU.GET);

            ResponseEvent event = snmp.send(pdu, target, null);
            return event != null && event.getResponse() != null;
        } catch (IOException e) {
            return false;
        }
    }

    private String getSnmpValue(Snmp snmp, CommunityTarget target, String oid) {
        try {
            PDU pdu = new PDU();
            pdu.add(new VariableBinding(new OID(oid)));
            pdu.setType(PDU.GET);

            ResponseEvent event = snmp.send(pdu, target, null);
            if (event != null && event.getResponse() != null) {
                PDU response = event.getResponse();
                if (response.getErrorStatus() == PDU.noError) {
                    Variable variable = response.get(0).getVariable();
                    return variable.toString();
                }
            }
        } catch (IOException e) {
            logger.error("Error getting SNMP value for OID {}: {}", oid, e.getMessage());
        }
        return null;
    }

    private String interpretPrinterStatus(String statusCode) {
        if (statusCode == null) return "UNKNOWN";

        try {
            int code = Integer.parseInt(statusCode);
            switch (code) {
                case 1: return "OTHER";
                case 2: return "UNKNOWN";
                case 3: return "IDLE";
                case 4: return "PRINTING";
                case 5: return "WARMUP";
                default: return "ONLINE";
            }
        } catch (NumberFormatException e) {
            return "ONLINE";
        }
    }

    public boolean testConnection(String ipAddress) {
        try {
            Snmp snmp = createSnmpSession();
            CommunityTarget target = createTarget(ipAddress);
            boolean reachable = isPrinterReachable(snmp, target);
            snmp.close();
            return reachable;
        } catch (Exception e) {
            logger.error("Error testing connection to {}: {}", ipAddress, e.getMessage());
            return false;
        }
    }
}