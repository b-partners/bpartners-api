package app.bpartners.api.repository.bridge;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.SecurityUtils.BEARER_PREFIX;

import app.bpartners.api.endpoint.rest.security.bridge.BridgeConf;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import app.bpartners.api.repository.bridge.model.Item.BridgeConnectItem;
import app.bpartners.api.repository.bridge.model.Item.BridgeCreateItem;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.repository.bridge.model.Item.BridgeItemStatus;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Data
public class BridgeApi {
  public static final String MAX_DAILY_REFRESHES_REACHED = "max_daily_refreshes_reached";
  private final HttpClient httpClient = HttpClient.newBuilder().build();

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private BridgeConf conf;

  public BridgeApi(BridgeConf conf) {
    this.conf = conf;
  }

  public BridgeUser findById(String uuid) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
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

  public Instant getItemStatusRefreshedAt(Long id, String token) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(conf.getItemStatusUrl(id)))
              .headers(defaultHeadersWithToken(token))
              .GET()
              .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 201) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper
          .readValue(httpResponse.body(), BridgeItemStatus.class)
          .getRefreshedAt()
          .truncatedTo(ChronoUnit.MILLIS);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BridgeConnectItem validateCurrentProItems(String token) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(conf.getProItemsValidationUrl()))
              .headers(defaultHeadersWithToken(token))
              .GET()
              .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 201) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper.readValue(httpResponse.body(), BridgeConnectItem.class);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BridgeConnectItem editItem(String token, Long itemId) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(conf.getEditItemsUrl() + itemId))
              .headers(defaultHeadersWithToken(token))
              .GET()
              .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 201) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper.readValue(httpResponse.body(), BridgeConnectItem.class);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BridgeConnectItem initiateScaSync(String token, Long itemId) {
    try {
      String queryParams = "?item_id=" + itemId;
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(conf.getScaSyncUrl() + queryParams))
              .headers(defaultHeadersWithToken(token))
              .GET()
              .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 201) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper.readValue(httpResponse.body(), BridgeConnectItem.class);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public String refreshBankConnection(Long itemId, String token) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(conf.getRefreshUrl(itemId)))
              .headers(defaultHeadersWithToken(token))
              .POST(HttpRequest.BodyPublishers.noBody())
              .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (itemHasReachMaxDailyRefresh(httpResponse)) {
        return null;
      }
      if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 201) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return null;
      }
      return objectMapper.readValue(httpResponse.body(), String.class);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private boolean itemHasReachMaxDailyRefresh(HttpResponse<String> httpResponse) {
    return httpResponse.body().contains(MAX_DAILY_REFRESHES_REACHED);
  }

  public List<BridgeUser> findAllUsers() {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
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
      BridgeListResponse<BridgeUser> response =
          objectMapper.readValue(httpResponse.body(), new TypeReference<>() {});
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
      HttpRequest request =
          HttpRequest.newBuilder()
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
      HttpRequest request =
          HttpRequest.newBuilder()
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
      BridgeListResponse<BridgeAccount> response =
          objectMapper.readValue(httpResponse.body(), new TypeReference<>() {});
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
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(conf.getUserUrl()))
              .headers(defaultHeadersWithJsonContentType())
              .POST(
                  HttpRequest.BodyPublishers.ofString(
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
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(conf.getAuthUrl()))
              .headers(defaultHeadersWithJsonContentType())
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(user)))
              .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 201) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        log.warn("Unauthenticated user is {}", user);
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
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(conf.getAddItemUrl()))
              .headers(requestHeaders.toArray(headers))
              .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(item)))
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
      HttpRequest request =
          HttpRequest.newBuilder()
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
      BridgeListResponse<BridgeItem> response =
          objectMapper.readValue(httpResponse.body(), new TypeReference<>() {});
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
      HttpRequest request =
          HttpRequest.newBuilder()
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

  public boolean deleteItem(Long id, String token) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(new URI(conf.getItemByIdUrl(String.valueOf(id))))
              .headers(defaultHeadersWithToken(token))
              .DELETE()
              .build();
      HttpResponse<String> httpResponse =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      if (httpResponse.statusCode() != 200 && httpResponse.statusCode() != 204) {
        log.warn("BridgeApi errors : {}", httpResponse.body());
        return false;
      }
      return true;
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private List<BridgeTransaction> getAllBridgeTransactionsByUserToken(
      String userToken, String uri, List<BridgeTransaction> transactions)
      throws URISyntaxException, IOException, InterruptedException {
    String requestUri =
        uri == null ? conf.getTransactionUrl() : conf.getPaginatedTransactionUrl(uri);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(new URI(requestUri))
            .headers(defaultHeadersWithToken(userToken))
            .GET()
            .build();
    HttpResponse<String> httpResponse =
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    if (httpResponse.statusCode() != 200) {
      log.warn("BridgeApi errors : {}", httpResponse.body());
      return List.of();
    }
    BridgeListResponse<BridgeTransaction> response =
        objectMapper.readValue(httpResponse.body(), new TypeReference<>() {});
    transactions.addAll(response.getResources());
    String nextUri = response.getPagination().getNextUri();
    if (nextUri == null) {
      return transactions;
    }
    return getAllBridgeTransactionsByUserToken(userToken, nextUri, transactions);
  }

  public List<BridgeTransaction> findTransactionsUpdatedByToken(String userToken) {
    try {
      List<BridgeTransaction> transactions = new ArrayList<>();
      return getAllBridgeTransactionsByUserToken(userToken, null, transactions);
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BridgeTransaction findTransactionByIdAndToken(Long id, String userToken) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
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
      HttpRequest request =
          HttpRequest.newBuilder()
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
      BridgeListResponse<BridgeBank> response =
          objectMapper.readValue(httpResponse.body(), new TypeReference<>() {});
      return response.getResources();
    } catch (URISyntaxException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public BridgeBank findBankById(Long id) {
    try {
      HttpRequest request =
          HttpRequest.newBuilder()
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
      "Bridge-Version", conf.getBridgeVersion()
    };
  }

  public String[] defaultHeadersWithToken(String token) {
    return new String[] {
      "Authorization", BEARER_PREFIX + token,
      "Client-Id", conf.getClientId(),
      "Client-Secret", conf.getClientSecret(),
      "Bridge-Version", conf.getBridgeVersion()
    };
  }

  List<String> addParams(ArrayList<String> headers, String paramName, String paramValue) {
    headers.add(paramName);
    headers.add(paramValue);
    return headers;
  }
}
