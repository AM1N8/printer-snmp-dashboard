package com.snmp.Monitoring.service;

import com.snmp.Monitoring.model.Printer;
import com.snmp.Monitoring.repository.PrinterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class PrinterService {

    private static final Logger logger = LoggerFactory.getLogger(PrinterService.class);

    @Autowired
    private PrinterRepository printerRepository;

    @Autowired
    private SnmpService snmpService;

    public List<Printer> getAllPrinters() {
        return printerRepository.findAll();
    }

    public Optional<Printer> getPrinterById(Long id) {
        return printerRepository.findById(id);
    }

    public Optional<Printer> getPrinterByIpAddress(String ipAddress) {
        return printerRepository.findByIpAddress(ipAddress);
    }

    public Printer savePrinter(Printer printer) {
        return printerRepository.save(printer);
    }

    public Printer addPrinter(String ipAddress, String name, String location) {
        // Check if printer already exists
        Optional<Printer> existingPrinter = printerRepository.findByIpAddress(ipAddress);
        if (existingPrinter.isPresent()) {
            throw new IllegalArgumentException("Printer with IP address " + ipAddress + " already exists");
        }

        // Test connection first
        if (!snmpService.testConnection(ipAddress)) {
            throw new IllegalArgumentException("Cannot connect to printer at " + ipAddress);
        }

        Printer printer = new Printer(ipAddress, name);
        if (location != null) {
            printer.setLocation(location);
        }

        printer = printerRepository.save(printer);

        // Update printer information asynchronously
        snmpService.updatePrinterStatus(printer)
                .thenAccept(updatedPrinter -> printerRepository.save(updatedPrinter));

        return printer;
    }

    public void deletePrinter(Long id) {
        printerRepository.deleteById(id);
    }

    public Printer updatePrinter(Long id, Printer updatedPrinter) {
        Optional<Printer> existingPrinter = printerRepository.findById(id);
        if (existingPrinter.isPresent()) {
            Printer printer = existingPrinter.get();
            printer.setName(updatedPrinter.getName());
            printer.setLocation(updatedPrinter.getLocation());
            // Don't update IP address to avoid conflicts
            return printerRepository.save(printer);
        }
        throw new IllegalArgumentException("Printer not found with id: " + id);
    }

    @Scheduled(fixedRate = 15000) // Run every 5 minutes
    public void updateAllPrinterStatuses() {
        logger.info("Starting scheduled update of all printer statuses");

        List<Printer> printers = printerRepository.findAll();

        List<CompletableFuture<Printer>> futures = printers.stream()
                .map(snmpService::updatePrinterStatus)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenRun(() -> {
                    futures.forEach(future -> {
                        try {
                            Printer updatedPrinter = future.get();
                            printerRepository.save(updatedPrinter);
                        } catch (Exception e) {
                            logger.error("Error saving updated printer: {}", e.getMessage());
                        }
                    });
                    logger.info("Completed scheduled update of all printer statuses");
                });
    }

    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalPrinters = printerRepository.count();
        long onlinePrinters = printerRepository.countOnlinePrinters();
        long offlinePrinters = printerRepository.countOfflinePrinters();
        long errorPrinters = printerRepository.countErrorPrinters();

        stats.put("totalPrinters", totalPrinters);
        stats.put("onlinePrinters", onlinePrinters);
        stats.put("offlinePrinters", offlinePrinters);
        stats.put("errorPrinters", errorPrinters);

        // Low toner/paper alerts
        List<Printer> lowTonerPrinters = printerRepository.findByTonerLevelLessThan(20);
        List<Printer> lowPaperPrinters = printerRepository.findByPaperLevelLessThan(20);

        stats.put("lowTonerPrinters", lowTonerPrinters.size());
        stats.put("lowPaperPrinters", lowPaperPrinters.size());
        stats.put("lowTonerList", lowTonerPrinters);
        stats.put("lowPaperList", lowPaperPrinters);

        return stats;
    }

    public List<Printer> getPrintersByStatus(String status) {
        return printerRepository.findByStatus(status);
    }

    public List<Printer> getPrintersByLocation(String location) {
        return printerRepository.findByLocation(location);
    }

    public void refreshPrinterStatus(Long id) {
        Optional<Printer> printerOpt = printerRepository.findById(id);
        if (printerOpt.isPresent()) {
            Printer printer = printerOpt.get();
            snmpService.updatePrinterStatus(printer)
                    .thenAccept(updatedPrinter -> printerRepository.save(updatedPrinter));
        }
    }
}
