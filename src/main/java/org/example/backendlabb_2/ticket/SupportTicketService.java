package org.example.backendlabb_2.ticket;

import org.example.backendlabb_2.user.UserAccount;
import org.example.backendlabb_2.user.UserAccountRepository;
import org.example.backendlabb_2.user.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupportTicketService {
    private final SupportTicketRepository tickets;
    private final TicketCommentRepository comments;
    private final UserAccountRepository users;

    public SupportTicketService(SupportTicketRepository tickets, TicketCommentRepository comments,
                                UserAccountRepository users) {
        this.tickets = tickets;
        this.comments = comments;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> findForUser(String username) {
        return tickets.findByOwnerUsernameOrderByCreatedAtDesc(username);
    }

    @Transactional(readOnly = true)
    public List<SupportTicket> findAll() {
        return tickets.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public SupportTicket findVisibleTicket(Long id, String username, boolean staff) {
        SupportTicket ticket = tickets.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        if (!staff && !ticket.getOwner().getUsername().equals(username)) {
            throw new TicketAccessDeniedException();
        }
        return ticket;
    }

    @Transactional
    public SupportTicket create(String title, String description, String username) {
        UserAccount owner = users.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        return tickets.save(new SupportTicket(title.trim(), description.trim(), owner));
    }

    @Transactional(readOnly = true)
    public List<TicketComment> findComments(Long ticketId) {
        return comments.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    @Transactional
    public void addComment(Long ticketId, String content, String username, boolean staff) {
        SupportTicket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        if (!staff && !ticket.getOwner().getUsername().equals(username)) {
            throw new TicketAccessDeniedException();
        }

        String normalizedContent = content.trim();
        if (normalizedContent.isEmpty() || normalizedContent.length() > 2000) {
            throw new IllegalArgumentException("Kommentaren måste innehålla 1–2000 tecken.");
        }

        UserAccount author = users.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        comments.save(new TicketComment(normalizedContent, ticket, author));
    }

    @Transactional
    public void updateStatus(Long id, TicketStatus status) {
        SupportTicket ticket = tickets.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        ticket.setStatus(status);
    }

    @Transactional
    public boolean claim(Long ticketId, String username) {
        UserAccount agent = users.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Authenticated user no longer exists"));
        if (agent.getRole() != Role.AGENT && agent.getRole() != Role.ADMIN) {
            throw new TicketAccessDeniedException();
        }

        int claimed = tickets.claimIfUnassigned(ticketId, agent);
        if (claimed == 1) {
            return true;
        }

        SupportTicket ticket = tickets.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        return ticket.getAssignedAgent() != null
                && ticket.getAssignedAgent().getUsername().equals(username);
    }
}
