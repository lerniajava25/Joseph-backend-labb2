package org.example.backendlabb_2.ticket;

public enum TicketStatus {
    NEW("Ny"),
    OPEN("Öppen"),
    IN_PROGRESS("Pågår"),
    WAITING_FOR_USER("Väntar på användare"),
    RESOLVED("Löst"),
    CLOSED("Stängt");

    private final String label;

    TicketStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
