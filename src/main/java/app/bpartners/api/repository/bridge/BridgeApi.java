package app.bpartners.api.repository.bridge;

import app.bpartners.api.endpoint.rest.security.swan.BridgeConf;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import app.bpartners.api.repository.bridge.model.Item.BridgeConnectItem;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.repository.bridge.model.Item.BridgeCreateItem;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.model.User.CreateBridgeUser;
import app.bpartners.api.repository.bridge.response.BridgeListResponse;
import app.bpartners.api.repository.bridge.response.BridgeTokenResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Component
@Slf4j
@Data
public class BridgeApi {
  private final HttpClient httpClient = HttpClient.newBuilder().build();

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private BridgeConf conf;

  public BridgeApi(BridgeConf conf) {
    this.conf = conf;
  }

  public BridgeUser findById(String uuid) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getUserUrl() + "/" + uuid))
          .headers(defaultHeaders())
          .GET()
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 201) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper.readValue(httpResponse.body(), BridgeUser.class);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public List<BridgeUser> findAllUsers() {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getUserUrl()))
          .headers(defaultHeadersWithJsonContentType())
          .GET()
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return List.of();
      }
      BridgeListResponse<BridgeUser> response = objectMapper.readValue(httpResponse.body(),
          new TypeReference<>() {
          });
      return response.getResources();
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BridgeAccount findByAccountById(Long id, String token) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getAccountUrl() + "/" + id))
          .headers(defaultHeadersWithToken(token))
          .GET()
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 201) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper.readValue(httpResponse.body(), BridgeAccount.class);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public List<BridgeAccount> findAccountsByToken(String token) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getAccountUrl()))
          .headers(defaultHeadersWithToken(token))
          .GET()
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return List.of();
      }
      BridgeListResponse<BridgeAccount> response = objectMapper.readValue(httpResponse.body(),
          new TypeReference<>() {
          });
      return response.getResources();
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BridgeUser createUser(CreateBridgeUser createBridgeUser) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getUserUrl()))
          .headers(defaultHeadersWithJsonContentType())
          .POST(HttpRequest.BodyPublishers.ofString(
              objectMapper.writeValueAsString(createBridgeUser)))
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 201) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper.readValue(httpResponse.body(), BridgeUser.class);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BridgeTokenResponse authenticateUser(CreateBridgeUser user) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getAuthUrl()))
          .headers(defaultHeadersWithJsonContentType())
          .POST(HttpRequest.BodyPublishers.ofString(
              objectMapper.writeValueAsString(user)))
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 201) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper.readValue(httpResponse.body(), BridgeTokenResponse.class);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public String initiateBankConnection(BridgeCreateItem item, String token) {
    try {
      ArrayList<String> requestHeaders =
          new ArrayList<>(Arrays.asList(defaultHeadersWithToken(token)));
      addParams(requestHeaders, "Content-Type", "application/json");
      String[] headers = new String[requestHeaders.size()];
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getAddItemUrl()))
          .headers(requestHeaders.toArray(headers))
          .POST(HttpRequest.BodyPublishers.ofString(
              objectMapper.writeValueAsString(item)))
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 201) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper.readValue(httpResponse.body(), BridgeConnectItem.class).getRedirectUrl();
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public List<BridgeItem> findItemsByToken(String token) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getGetItemUrl()))
          .headers(defaultHeadersWithToken(token))
          .GET()
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return List.of();
      }
      BridgeListResponse<BridgeItem> response = objectMapper.readValue(httpResponse.body(),
          new TypeReference<>() {
          });
      return response.getResources();
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BridgeItem findItemByIdAndToken(String id, String token) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getItemByIdUrl(id)))
          .headers(defaultHeadersWithToken(token))
          .GET()
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper.readValue(httpResponse.body(), BridgeItem.class);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public List<BridgeTransaction> findTransactionsUpdatedByToken(String userToken) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getTransactionUpdatedUrl()))
          .headers(defaultHeadersWithToken(userToken))
          .GET()
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return List.of();
      }
      BridgeListResponse<BridgeTransaction> response = objectMapper.readValue(httpResponse.body(),
          new TypeReference<>() {
          });
      return response.getResources();
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BridgeTransaction findTransactionByIdAndToken(Long id, String userToken) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getTransactionUrl() + "/" + id))
          .headers(defaultHeadersWithToken(userToken))
          .GET()
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper.readValue(httpResponse.body(), BridgeTransaction.class);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public List<BridgeBank> findAllBanks() {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getBankUrl()))
          .headers(defaultHeadersWithJsonContentType())
          .GET()
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return List.of();
      }
      BridgeListResponse<BridgeBank> response = objectMapper.readValue(httpResponse.body(),
          new TypeReference<>() {
          });
      return response.getResources();
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BridgeBank findBankById(Integer id) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(conf.getBankUrl() + "/" + id))
          .headers(defaultHeadersWithJsonContentType())
          .GET()
          .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper.readValue(httpResponse.body(), BridgeBank.class);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public String[] defaultHeaders() {
    return new String[] {
        "Client-Id", conf.getClientId(),
        "Client-Secret", conf.getClientSecret(),
        "Bridge-Version", conf.getBridgeVersion()
    };
  }

  public String[] defaultHeadersWithJsonContentType() {
    return new String[] {
        "Content-Type", "application/json",
        "Client-Id", conf.getClientId(),
        "Client-Secret", conf.getClientSecret(),
        "Bridge-Version", conf.getBridgeVersion()};
  }

  public String[] defaultHeadersWithToken(String token) {
    return new String[] {
        "Authorization", BEARER_PREFIX + token,
        "Client-Id", conf.getClientId(),
        "Client-Secret", conf.getClientSecret(),
        "Bridge-Version", conf.getBridgeVersion()};
  }

  List<String> addParams(ArrayList<String> headers, String paramName, String paramValue) {
    headers.add(paramName);
    headers.add(paramValue);
    return headers;
  }
}
