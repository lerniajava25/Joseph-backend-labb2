package org.example.backendlabb_2.web;

import jakarta.validation.Valid;
import org.example.backendlabb_2.ticket.SupportTicket;
import org.example.backendlabb_2.ticket.SupportTicketService;
import org.example.backendlabb_2.ticket.TicketStatus;
import org.example.backendlabb_2.user.UserAccountService;
import org.example.backendlabb_2.user.UsernameAlreadyExistsException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {
    private final UserAccountService accounts;
    private final SupportTicketService tickets;

    public PageController(UserAccountService accounts, SupportTicketService tickets) {
        this.accounts = accounts;
        this.tickets = tickets;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form, BindingResult result) {
        if (result.hasErrors()) {
            return "register";
        }
        try {
            accounts.register(form.getUsername(), form.getDisplayName(), form.getPassword());
        } catch (UsernameAlreadyExistsException exception) {
            result.rejectValue("username", "duplicate", "Användarnamnet används redan.");
            return "register";
        }
        return "redirect:/login?registered";
    }

    @GetMapping("/tickets")
    public String myTickets(Authentication authentication, Model model) {
        model.addAttribute("tickets", tickets.findForUser(authentication.getName()));
        model.addAttribute("isAdmin", isAdmin(authentication));
        model.addAttribute("adminView", false);
        return "tickets";
    }

    @GetMapping("/tickets/new")
    public String newTicket(Model model) {
        model.addAttribute("form", new TicketForm());
        return "ticket-form";
    }

    @PostMapping("/tickets")
    public String createTicket(@Valid @ModelAttribute("form") TicketForm form, BindingResult result,
                               Authentication authentication) {
        if (result.hasErrors()) {
            return "ticket-form";
        }
        SupportTicket ticket = tickets.create(form.getTitle(), form.getDescription(), authentication.getName());
        return "redirect:/tickets/" + ticket.getId();
    }

    @GetMapping("/tickets/{id}")
    public String ticket(@PathVariable Long id, Authentication authentication, Model model) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        model.addAttribute("ticket", tickets.findVisibleTicket(id, authentication.getName(), admin));
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("admin", admin);
        return "ticket-detail";
    }

    @GetMapping("/admin/tickets")
    @PreAuthorize("hasRole('ADMIN')")
    public String allTickets(Model model) {
        model.addAttribute("tickets", tickets.findAll());
        model.addAttribute("isAdmin", true);
        model.addAttribute("adminView", true);
        return "tickets";
    }

    @PostMapping("/admin/tickets/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateStatus(@PathVariable Long id, @RequestParam TicketStatus status,
                               RedirectAttributes redirectAttributes) {
        tickets.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("message", "Ärendets status har uppdaterats.");
        return "redirect:/tickets/" + id;
    }

    private boolean isAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
