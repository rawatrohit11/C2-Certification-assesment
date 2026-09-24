package com.example.supportticket.ticket.persistence;

import com.example.supportticket.ticket.domain.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TicketRepository extends JpaRepository<TicketEntity, UUID> {

    Page<TicketEntity> findByTitleContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String title,
            String description,
            Pageable pageable
    );

    Page<TicketEntity> findByStatus(TicketStatus status, Pageable pageable);

    Page<TicketEntity>
    findByStatusAndTitleContainingIgnoreCaseOrStatusAndDescriptionContainingIgnoreCase(
            TicketStatus titleStatus,
            String title,
            TicketStatus descriptionStatus,
            String description,
            Pageable pageable
    );
}
