package app.bpartners.api.endpoint.rest.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenConf;
import app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenIssuer;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.User;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TokenControllerTest {
  private static final String USER_ID = "user-id";

  DashboardTokenIssuer issuer =
      new DashboardTokenIssuer(
          new DashboardTokenConf("test-dashboard-token-signing-key-0123456789", 15));
  MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TokenController(issuer)).build();

  @AfterEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void post_token_returns_token_for_authenticated_user() throws Exception {
    var principal = new Principal(User.builder().id(USER_ID).roles(List.of()).build(), "api-key");
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(principal, "api-key", List.of()));

    mockMvc
        .perform(post("/token"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.token").isNotEmpty())
        .andExpect(jsonPath("$.expiresAt").exists());
  }
}
