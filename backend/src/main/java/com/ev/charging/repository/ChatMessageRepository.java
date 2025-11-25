package com.ev.charging.repository;

import com.ev.charging.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByStationIdAndIsDeletedOrderByCreateTimeDesc(
        Long stationId,
        Byte isDeleted,
        Pageable pageable
    );
}
