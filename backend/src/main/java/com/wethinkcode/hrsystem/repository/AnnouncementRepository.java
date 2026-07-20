package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @Query("SELECT a FROM Announcement a WHERE a.expiryDate IS NULL OR a.expiryDate >= :today ORDER BY a.postedDate DESC")
    List<Announcement> findActive(LocalDate today);
}