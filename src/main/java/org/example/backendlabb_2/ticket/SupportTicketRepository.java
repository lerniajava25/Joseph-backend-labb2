package org.example.backendlabb_2.ticket;

import org.example.backendlabb_2.user.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupportTicketRepository extends JpaRepository<SupportTicket, Long> {
    List<SupportTicket> findByOwnerUsernameOrderByCreatedAtDesc(String username);

    @EntityGraph(attributePaths = {"owner", "assignedAgent"})
    List<SupportTicket> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"owner", "assignedAgent"})
    Optional<SupportTicket> findById(Long id);

    @Modifying
    @Query("""
            update SupportTicket ticket
            set ticket.assignedAgent = :agent, ticket.lastModified = :modifiedAt
            where ticket.id = :ticketId and ticket.assignedAgent is null
            """)
    int claimIfUnassigned(@Param("ticketId") Long ticketId, @Param("agent") UserAccount agent,
                          @Param("modifiedAt") java.time.LocalDateTime modifiedAt);
}
