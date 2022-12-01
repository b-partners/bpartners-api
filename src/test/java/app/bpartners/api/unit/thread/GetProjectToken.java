package app.bpartners.api.unit.thread;

import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import java.io.IOException;
import java.net.http.HttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpResponseFactory;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class GetProjectToken {
  private SwanApi swanApi;
  private PrincipalProvider auth;
  private SwanCustomApi swanCustomApi;
  private SwanConf swanConf;
  private HttpClient httpClient;
  private HttpResponseFactory responseFactory = new DefaultHttpResponseFactory();
  private int n = 1;

  @BeforeEach
  void setUp() throws IOException, InterruptedException {
    auth = mock(PrincipalProvider.class);
    swanConf = mock(SwanConf.class);
    httpClient = mock(HttpClient.class);
    swanApi = new SwanApi(auth, swanCustomApi, swanConf);
    when(httpClient.send(any(), any()))
        .thenThrow(new IOException(""));
  }

  @Test
  void testThread() {
    assertThrows(ApiException.class, () ->
        swanApi.getProjectToken(n, httpClient, 100)
    );
  }
}
