package app.bpartners.api.repository.expressif;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.expressif.model.InputForm;
import app.bpartners.api.repository.expressif.model.OutputValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExpressifApi {
  public static final String SSL_PROTOCOL = "TLSv1.2";
  private final HttpClient httpClient =
      HttpClient.newBuilder().sslContext(mockedSslContext()).build();
  private final ObjectMapper objectMapper =
      new ObjectMapper()
          .findAndRegisterModules()
          .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
  private final ExpressifConf conf;

  public ExpressifApi(ExpressifConf conf) {
    this.conf = conf;
  }

  public List<OutputValue> process(InputForm inputForm) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(conf.getProcessUrl(defaultParams())))
              .headers("Authorization", "Bearer " + conf.getProjectToken())
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(inputForm)))
              .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        log.warn("Expressif errors : {}", response.body());
        return List.of();
      }
      return objectMapper.readValue(response.body(), new TypeReference<>() {});
    } catch (IOException | URISyntaxException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private HashMap<String, String> defaultParams() {
    HashMap<String, String> queryParams = new HashMap<>();
    queryParams.put("querier", "historic_data");
    queryParams.put("ruleBase", "Depanneurs.rules"); // TODO: set customizable
    return queryParams;
  }

  @SneakyThrows
  private static SSLContext mockedSslContext() {
    SSLContext sslContext = SSLContext.getInstance(SSL_PROTOCOL);
    sslContext.init(null, new TrustManager[] {MOCK_TRUST_MANAGER}, new SecureRandom());
    return sslContext;
  }

  private static final TrustManager MOCK_TRUST_MANAGER =
      new X509ExtendedTrustManager() {

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) {
          // Do nothing
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) {
          // Do nothing

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
          // Do nothing
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
          return new X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) {
          // Do nothing
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) {
          // Do nothing

        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) {
          // Do nothing
        }
      };
}
