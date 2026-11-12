package org.example.backendlabb_2.user;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException() {
        super("An account with that username already exists");
    }
}
