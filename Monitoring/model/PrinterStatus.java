package com.snmp.Monitoring.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "printer_status")
public class PrinterStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "printer_id", nullable = false)
    private Printer printer;

    private String status;
    private Integer tonerLevel;
    private Integer paperLevel;
    private Integer totalPagesPrinted;
    private String errorMessage;
    private LocalDateTime timestamp;

    // Constructors
    public PrinterStatus() {
        this.timestamp = LocalDateTime.now();
    }

    public PrinterStatus(Printer printer, String status) {
        this();
        this.printer = printer;
        this.status = status;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Printer getPrinter() {
        return printer;
    }

    public void setPrinter(Printer printer) {
        this.printer = printer;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTonerLevel() {
        return tonerLevel;
    }

    public void setTonerLevel(Integer tonerLevel) {
        this.tonerLevel = tonerLevel;
    }

    public Integer getPaperLevel() {
        return paperLevel;
    }

    public void setPaperLevel(Integer paperLevel) {
        this.paperLevel = paperLevel;
    }

    public Integer getTotalPagesPrinted() {
        return totalPagesPrinted;
    }

    public void setTotalPagesPrinted(Integer totalPagesPrinted) {
        this.totalPagesPrinted = totalPagesPrinted;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}