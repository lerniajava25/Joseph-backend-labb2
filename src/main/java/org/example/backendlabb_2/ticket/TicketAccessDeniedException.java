package org.example.backendlabb_2.ticket;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class TicketAccessDeniedException extends RuntimeException {
    public TicketAccessDeniedException() {
        super("You are not allowed to view this ticket");
    }
}
