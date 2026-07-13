package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findBySenderIdOrRecipientIdOrderBySentAtDesc(Long senderId, Long recipientId);
    List<Message> findByRecipientIdAndIsReadFalse(Long recipientId);
}