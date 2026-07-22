package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.HearingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HearingParticipantRepository extends JpaRepository<HearingParticipant, Long> {
    List<HearingParticipant> findByHearingId(Long hearingId);
}