package org.example.backendlabb_2.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class UserAccountService {
    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserAccountRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void register(String username, String displayName, String rawPassword) {
        String normalizedUsername = normalizeUsername(username);
        if (users.existsByUsername(normalizedUsername)) {
            throw new UsernameAlreadyExistsException();
        }
        users.save(new UserAccount(normalizedUsername, passwordEncoder.encode(rawPassword), displayName.trim(), Role.USER));
    }

    public static String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
