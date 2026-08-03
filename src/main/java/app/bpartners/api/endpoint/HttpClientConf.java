package app.bpartners.api.endpoint;

import java.net.http.HttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HttpClientConf {

  @Bean
  public HttpClient httpClient() {
    return HttpClient.newHttpClient();
  }
}
