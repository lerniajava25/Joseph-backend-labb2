package org.example.backendlabb_2.web;

import org.example.backendlabb_2.user.UserAccountRepository;
import org.example.backendlabb_2.user.UserAccount;
import org.example.backendlabb_2.user.Role;
import org.example.backendlabb_2.user.UserAccountService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class WebSecurityConfig {
    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/register", "/css/**", "/error").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/tickets", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
                .build();
    }

    @Bean
    UserDetailsService userDetailsService(UserAccountRepository users) {
        return username -> users.findByUsername(UserAccountService.normalizeUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    ApplicationRunner provisionInitialAdmin(UserAccountRepository users, PasswordEncoder encoder,
                                              @Value("${app.initial-admin.username:}") String username,
                                              @Value("${app.initial-admin.password:}") String password) {
        return args -> {
            if (username.isBlank() && password.isBlank()) {
                return;
            }
            if (username.isBlank() || password.isBlank()) {
                throw new IllegalStateException("Both INITIAL_ADMIN_USERNAME and INITIAL_ADMIN_PASSWORD must be set");
            }
            if (password.length() < 12) {
                throw new IllegalStateException("Initial administrator password must contain at least 12 characters");
            }
            String normalizedUsername = UserAccountService.normalizeUsername(username);
            UserAccount account = users.findByUsername(normalizedUsername).orElse(null);
            if (account == null) {
                users.save(new UserAccount(normalizedUsername, encoder.encode(password), "Administratör", Role.ADMIN));
            } else if (account.getRole() != Role.ADMIN) {
                throw new IllegalStateException("Initial administrator username belongs to a non-admin account");
            }
        };
    }
}
