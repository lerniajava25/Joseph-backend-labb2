package org.example.backendlabb_2.ticket;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByOwnerUsernameOrderByCreatedAtDesc(String username);

    @EntityGraph(attributePaths = "owner")
    List<SupportTicket> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = "owner")
    Optional<SupportTicket> findById(Long id);
}
