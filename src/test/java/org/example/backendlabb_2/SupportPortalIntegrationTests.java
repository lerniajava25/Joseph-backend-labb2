package org.example.backendlabb_2;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

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

    @Test
    void usersCanRegisterAndOnlyAccessTheirOwnTickets() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().isOk());
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

        MvcResult bobLogin = mockMvc.perform(formLogin().user("username", "bob").password("password", "password123"))
                .andExpect(authenticated())
                .andReturn();
        MockHttpSession bobSession = (MockHttpSession) bobLogin.getRequest().getSession(false);
        mockMvc.perform(get(ticketUrl).session(bobSession))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/tickets").session(bobSession))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(ticketUrl).session(aliceSession))
                .andExpect(status().isOk());

        MvcResult adminLogin = mockMvc.perform(formLogin().user("username", "admin")
                        .password("password", "TestAdminPassword123"))
                .andExpect(authenticated())
                .andReturn();
        MockHttpSession adminSession = (MockHttpSession) adminLogin.getRequest().getSession(false);
        mockMvc.perform(get("/admin/tickets").session(adminSession))
                .andExpect(status().isOk());
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
