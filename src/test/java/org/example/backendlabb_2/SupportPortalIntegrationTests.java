package org.example.backendlabb_2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.example.backendlabb_2.user.Role;
import org.example.backendlabb_2.user.UserAccountRepository;
import org.example.backendlabb_2.user.UserAccountService;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:support-tests;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.initial-admin.username=admin",
        "app.initial-admin.password=TestAdminPassword123"
})
@AutoConfigureMockMvc
class SupportPortalIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountService accounts;

    @Autowired
    private UserAccountRepository users;

    @Test
    void usersCanRegisterAndOnlyAccessTheirOwnTickets() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
        accounts.createByAdmin("agent", "Supportagent", "password123", Role.AGENT);
        register("alice", "Alice");
        register("bob", "Bob");

        MvcResult aliceLogin = mockMvc.perform(formLogin().user("username", "ALICE").password("password", "password123"))
                .andExpect(authenticated())
                .andReturn();
        MockHttpSession aliceSession = (MockHttpSession) aliceLogin.getRequest().getSession(false);
        MvcResult ticketCreated = mockMvc.perform(post("/tickets")
                        .session(aliceSession)
                        .with(csrf())
                        .param("title", "Login problem")
                        .param("description", "I cannot sign in"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String ticketUrl = ticketCreated.getResponse().getRedirectedUrl();
        mockMvc.perform(get("/tickets").session(aliceSession))
                .andExpect(status().isOk());
        mockMvc.perform(post(ticketUrl + "/comments")
                        .session(aliceSession)
                        .with(csrf())
                        .param("content", "Jag har startat om datorn"))
                .andExpect(status().is3xxRedirection());

        MvcResult bobLogin = mockMvc.perform(formLogin().user("username", "bob").password("password", "password123"))
                .andExpect(authenticated())
                .andReturn();
        MockHttpSession bobSession = (MockHttpSession) bobLogin.getRequest().getSession(false);
        mockMvc.perform(get(ticketUrl).session(bobSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ticketUrl + "/comments")
                        .session(bobSession)
                        .with(csrf())
                        .param("content", "Obehörig kommentar"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/agent/tickets/" + ticketUrl.substring(ticketUrl.lastIndexOf('/') + 1) + "/claim")
                        .session(bobSession)
                        .with(csrf()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/tickets").session(bobSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ticketUrl).session(aliceSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Jag har startat om datorn")));

        MvcResult agentLogin = mockMvc.perform(formLogin().user("username", "agent").password("password", "password123"))
                .andExpect(authenticated())
                .andReturn();
        MockHttpSession agentSession = (MockHttpSession) agentLogin.getRequest().getSession(false);
        mockMvc.perform(get("/admin/tickets").session(agentSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Login problem")));
        mockMvc.perform(get("/admin/users").session(agentSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(ticketUrl).session(agentSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tilldela mig ärendet")));
        mockMvc.perform(post("/agent/tickets/" + ticketUrl.substring(ticketUrl.lastIndexOf('/') + 1) + "/claim")
                        .session(agentSession)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get(ticketUrl).session(agentSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ansvarig agent: Supportagent")));
        mockMvc.perform(post(ticketUrl + "/comments")
                        .session(agentSession)
                        .with(csrf())
                        .param("content", "Jag tar hand om ärendet"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/tickets/" + ticketUrl.substring(ticketUrl.lastIndexOf('/') + 1) + "/status")
                        .session(agentSession)
                        .with(csrf())
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().is3xxRedirection());

        MvcResult adminLogin = mockMvc.perform(formLogin().user("username", "admin")
                        .password("password", "TestAdminPassword123"))
                .andExpect(authenticated())
                .andReturn();
        MockHttpSession adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);
        mockMvc.perform(get("/admin/tickets").session(adminSession))
                .andExpect(status().isOk());
        mockMvc.perform(get("/admin/users").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Hantera användare")));
        mockMvc.perform(post("/admin/users")
                        .session(adminSession)
                        .with(csrf())
                        .param("username", "newagent")
                        .param("displayName", "Ny agent")
                        .param("password", "password123")
                        .param("role", "AGENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        Long bobId = users.findByUsername("bob").orElseThrow().getId();
        mockMvc.perform(post("/admin/users/" + bobId + "/role")
                        .session(adminSession)
                        .with(csrf())
                        .param("role", "AGENT"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/admin/users/" + bobId + "/enabled")
                        .session(adminSession)
                        .with(csrf())
                        .param("enabled", "false"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(formLogin().user("username", "bob").password("password", "password123"))
                .andExpect(unauthenticated());
        mockMvc.perform(post("/admin/users/" + bobId + "/enabled")
                        .session(adminSession)
                        .with(csrf())
                        .param("enabled", "true"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(formLogin().user("username", "bob").password("password", "password123"))
                .andExpect(authenticated());
        String ticketId = ticketUrl.substring(ticketUrl.lastIndexOf('/') + 1);
        mockMvc.perform(post("/admin/tickets/" + ticketId + "/status")
                        .session(adminSession)
                        .with(csrf())
                        .param("status", "IN_PROGRESS"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get(ticketUrl).session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pågår")));
    }

    @Test
    void unauthenticatedVisitorsAreRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/tickets"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(unauthenticated());
    }

    private void register(String username, String displayName) throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", username)
                        .param("displayName", displayName)
                        .param("password", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));
    }
}
