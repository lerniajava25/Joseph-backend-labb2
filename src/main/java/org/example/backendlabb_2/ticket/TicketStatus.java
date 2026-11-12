package org.example.backendlabb_2.ticket;

public enum TicketStatus {
    OPEN("Öppen"),
    IN_PROGRESS("Pågår"),
    RESOLVED("Löst");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
