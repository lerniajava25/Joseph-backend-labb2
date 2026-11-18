package org.example.backendlabb_2.web;

import jakarta.validation.Valid;
import org.example.backendlabb_2.ticket.SupportTicket;
import org.example.backendlabb_2.ticket.SupportTicketService;
import org.example.backendlabb_2.ticket.TicketStatus;
import org.example.backendlabb_2.user.Role;
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
        model.addAttribute("isStaff", isStaff(authentication));
        model.addAttribute("isAdmin", hasRole(authentication, "ROLE_ADMIN"));
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
        boolean staff = isStaff(authentication);
        SupportTicket ticket = tickets.findVisibleTicket(id, authentication.getName(), staff);
        model.addAttribute("ticket", ticket);
        model.addAttribute("statuses", TicketStatus.values());
        model.addAttribute("staff", staff);
        model.addAttribute("canClaim", staff && (ticket.getAssignedAgent() == null
                || ticket.getAssignedAgent().getUsername().equals(authentication.getName())));
        model.addAttribute("canComment", staff || ticket.getOwner().getUsername().equals(authentication.getName()));
        model.addAttribute("comments", tickets.findComments(id));
        model.addAttribute("commentForm", new CommentForm());
        return "ticket-detail";
    }

    @PostMapping("/tickets/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @Valid @ModelAttribute("commentForm") CommentForm form,
                             BindingResult result,
                             Authentication authentication,
                             Model model) {
        boolean staff = isStaff(authentication);
        SupportTicket ticket = tickets.findVisibleTicket(id, authentication.getName(), staff);
        if (result.hasErrors()) {
            model.addAttribute("ticket", ticket);
            model.addAttribute("statuses", TicketStatus.values());
            model.addAttribute("staff", staff);
            model.addAttribute("canClaim", staff && (ticket.getAssignedAgent() == null
                    || ticket.getAssignedAgent().getUsername().equals(authentication.getName())));
            model.addAttribute("canComment", staff
                    || ticket.getOwner().getUsername().equals(authentication.getName()));
            model.addAttribute("comments", tickets.findComments(id));
            return "ticket-detail";
        }

        tickets.addComment(id, form.getContent(), authentication.getName(), staff);
        return "redirect:/tickets/" + id;
    }

    @PostMapping("/agent/tickets/{id}/claim")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public String claimTicket(@PathVariable Long id, Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        boolean claimed = tickets.claim(id, authentication.getName());
        redirectAttributes.addFlashAttribute("message", claimed
                ? "Ärendet är tilldelat dig."
                : "Ärendet har redan tilldelats en annan agent.");
        return "redirect:/tickets/" + id;
    }

    @GetMapping("/admin/tickets")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public String allTickets(Model model) {
        model.addAttribute("tickets", tickets.findAll());
        model.addAttribute("isStaff", true);
        model.addAttribute("isAdmin", true);
        model.addAttribute("adminView", true);
        return "tickets";
    }

    @GetMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String users(Model model) {
        model.addAttribute("users", accounts.findAll());
        model.addAttribute("roles", Role.values());
        model.addAttribute("form", new AdminUserForm());
        return "admin-users";
    }

    @PostMapping("/admin/users")
    @PreAuthorize("hasRole('ADMIN')")
    public String createUser(@Valid @ModelAttribute("form") AdminUserForm form,
                             BindingResult result, Model model,
                             RedirectAttributes redirectAttributes) {
        if (!result.hasErrors()) {
            try {
                accounts.createByAdmin(form.getUsername(), form.getDisplayName(),
                        form.getPassword(), form.getRole());
                redirectAttributes.addFlashAttribute("message", "Användaren har skapats.");
                return "redirect:/admin/users";
            } catch (UsernameAlreadyExistsException exception) {
                result.rejectValue("username", "duplicate", "Användarnamnet används redan.");
            }
        }

        model.addAttribute("users", accounts.findAll());
        model.addAttribute("roles", Role.values());
        return "admin-users";
    }

    @PostMapping("/admin/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public String changeUserRole(@PathVariable Long id, @RequestParam Role role,
                                 RedirectAttributes redirectAttributes) {
        accounts.changeRole(id, role);
        redirectAttributes.addFlashAttribute("message", "Användarens roll har ändrats.");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/users/{id}/enabled")
    @PreAuthorize("hasRole('ADMIN')")
    public String setUserEnabled(@PathVariable Long id, @RequestParam boolean enabled,
                                 RedirectAttributes redirectAttributes) {
        accounts.setEnabled(id, enabled);
        redirectAttributes.addFlashAttribute("message", enabled
                ? "Användarkontot har aktiverats."
                : "Användarkontot har inaktiverats.");
        return "redirect:/admin/users";
    }

    @PostMapping("/admin/tickets/{id}/status")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public String updateStatus(@PathVariable Long id, @RequestParam TicketStatus status,
                               RedirectAttributes redirectAttributes) {
        tickets.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("message", "Ärendets status har uppdaterats.");
        return "redirect:/tickets/" + id;
    }

    private boolean isStaff(Authentication authentication) {
        return hasRole(authentication, "ROLE_AGENT") || hasRole(authentication, "ROLE_ADMIN");
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> role.equals(authority.getAuthority()));
    }
}
