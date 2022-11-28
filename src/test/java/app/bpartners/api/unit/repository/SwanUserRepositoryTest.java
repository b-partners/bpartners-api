package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.security.principal.PrincipalProviderImpl;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.implementation.UserSwanRepositoryImpl;
import app.bpartners.api.repository.swan.model.SwanUser;
import app.bpartners.api.repository.swan.response.UserResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static app.bpartners.api.integration.conf.TestUtils.joeDoe;
import static app.bpartners.api.integration.conf.TestUtils.setUpProvider;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwanUserRepositoryTest {
  @Value("${test.user.access.token}")
  private static String bearer;
  PrincipalProviderImpl provider;
  SwanConf swanConf;
  SwanCustomApi<UserResponse> swanCustomApi;
  SwanApi<UserResponse> swanApi;
  UserSwanRepositoryImpl userSwanRepository;

  @BeforeEach
  void setUp() {
    provider = mock(PrincipalProviderImpl.class);
    setUpProvider(provider);
    swanConf = mock(SwanConf.class);
    swanCustomApi = mock(SwanCustomApi.class);
    swanApi = new SwanApi<>(provider, swanCustomApi, swanConf);
    when(swanCustomApi.getData(any(), any(), any())).thenReturn(
        userResponse());
    userSwanRepository = new UserSwanRepositoryImpl(swanApi, swanCustomApi);
  }

  @Test
  void read_swan_user_whoami_ok() {
    SwanUser actual = userSwanRepository.whoami();

    assertNotNull(actual);
  }

  @Test
  void read_swan_user_by_token_ok() {
    SwanUser actual = userSwanRepository.getByToken(bearer);

    assertNotNull(actual);
  }

  private UserResponse userResponse() {
    return UserResponse.builder()
        .data(new UserResponse.Data(joeDoe()))
        .build();
  }
}
