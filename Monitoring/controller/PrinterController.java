package com.snmp.Monitoring.controller;

import com.snmp.Monitoring.model.Printer;
import com.snmp.Monitoring.service.PrinterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/printers")
@CrossOrigin(origins = "*")
public class PrinterController {

    @Autowired
    private PrinterService printerService;

    @GetMapping
    public ResponseEntity<List<Printer>> getAllPrinters() {
        List<Printer> printers = printerService.getAllPrinters();
        return ResponseEntity.ok(printers);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Printer> getPrinterById(@PathVariable Long id) {
        Optional<Printer> printer = printerService.getPrinterById(id);
        return printer.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/ip/{ipAddress}")
    public ResponseEntity<Printer> getPrinterByIpAddress(@PathVariable String ipAddress) {
        Optional<Printer> printer = printerService.getPrinterByIpAddress(ipAddress);
        return printer.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> addPrinter(@RequestBody PrinterRequest request) {
        try {
            Printer printer = printerService.addPrinter(
                    request.getIpAddress(),
                    request.getName(),
                    request.getLocation()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(printer);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updatePrinter(@PathVariable Long id, @RequestBody Printer printer) {
        try {
            Printer updatedPrinter = printerService.updatePrinter(id, printer);
            return ResponseEntity.ok(updatedPrinter);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deletePrinter(@PathVariable Long id) {
        try {
            printerService.deletePrinter(id);
            return ResponseEntity.ok(Map.of("message", "Printer deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Printer>> getPrintersByStatus(@PathVariable String status) {
        List<Printer> printers = printerService.getPrintersByStatus(status);
        return ResponseEntity.ok(printers);
    }

    @GetMapping("/location/{location}")
    public ResponseEntity<List<Printer>> getPrintersByLocation(@PathVariable String location) {
        List<Printer> printers = printerService.getPrintersByLocation(location);
        return ResponseEntity.ok(printers);
    }

    @GetMapping("/dashboard/statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics() {
        Map<String, Object> stats = printerService.getDashboardStatistics();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/{id}/refresh")
    public ResponseEntity<?> refreshPrinterStatus(@PathVariable Long id) {
        try {
            printerService.refreshPrinterStatus(id);
            return ResponseEntity.ok(Map.of("message", "Printer status refresh initiated"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // Inner class for request body
    public static class PrinterRequest {
        private String ipAddress;
        private String name;
        private String location;

        // Getters and Setters
        public String getIpAddress() {
            return ipAddress;
        }

        public void setIpAddress(String ipAddress) {
            this.ipAddress = ipAddress;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLocation() {
            return location;
        }

        public void setLocation(String location) {
            this.location = location;
        }
    }
}