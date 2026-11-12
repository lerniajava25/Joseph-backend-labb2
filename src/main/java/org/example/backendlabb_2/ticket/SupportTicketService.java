package org.example.backendlabb_2.ticket;

import org.example.backendlabb_2.user.UserAccount;
import org.example.backendlabb_2.user.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SupportTicketService {
    private final SupportTicketRepository tickets;
    private final UserAccountRepository users;

    public SupportTicketService(SupportTicketRepository tickets, UserAccountRepository users) {
        this.tickets = tickets;
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
    public SupportTicket findVisibleTicket(Long id, String username, boolean admin) {
        SupportTicket ticket = tickets.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        if (!admin && !ticket.getOwner().getUsername().equals(username)) {
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

    @Transactional
    public void updateStatus(Long id, TicketStatus status) {
        SupportTicket ticket = tickets.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        ticket.setStatus(status);
    }
}
