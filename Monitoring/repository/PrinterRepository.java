package com.snmp.Monitoring.repository;

import com.snmp.Monitoring.model.Printer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrinterRepository extends JpaRepository<Printer, Long> {

    Optional<Printer> findByIpAddress(String ipAddress);

    List<Printer> findByStatus(String status);

    @Query("SELECT p FROM Printer p WHERE p.tonerLevel IS NOT NULL AND p.tonerLevel < :threshold")
    List<Printer> findByTonerLevelLessThan(Integer threshold);

    @Query("SELECT p FROM Printer p WHERE p.paperLevel IS NOT NULL AND p.paperLevel < :threshold")
    List<Printer> findByPaperLevelLessThan(Integer threshold);

    @Query("SELECT COUNT(p) FROM Printer p WHERE p.status = 'ONLINE'")
    Long countOnlinePrinters();

    @Query("SELECT COUNT(p) FROM Printer p WHERE p.status = 'OFFLINE'")
    Long countOfflinePrinters();

    @Query("SELECT COUNT(p) FROM Printer p WHERE p.status = 'ERROR'")
    Long countErrorPrinters();

    @Query("SELECT p FROM Printer p WHERE p.location = :location")
    List<Printer> findByLocation(String location);
}