package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import app.bpartners.api.repository.swan.model.Transaction;
import app.bpartners.api.repository.swan.response.OneTransactionResponse;
import app.bpartners.api.repository.swan.response.TransactionResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Repository
@Slf4j
@AllArgsConstructor
public class TransactionSwanRepositoryImpl implements TransactionSwanRepository {
  public static final String QUERY = "{"
      + "  \"query\": \"query TransactionAccount { account(accountId: \\\"%s\\\") { transactions { "
      + "edges { node { id label reference amount { currency value }"
      + " createdAt side statusInfo { status } } } } }}\"}";

  private final ProjectTokenManager tokenManager;
  private final SwanConf swanConf;
  private final SwanCustomApi<TransactionResponse> swanCustomApi;

  @Override
  public List<Transaction> getByIdAccount(String idAccount) {
    String query = String.format(QUERY, idAccount);
    return swanCustomApi.getData(TransactionResponse.class, query,
            tokenManager.getSwanProjecToken())
        .getData().getAccount().getTransactions().getEdges();
  }

  @Override
  public Transaction findById(String id) {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      String query =
          "{\"query\": \"query TransactionById { transaction(id: \\\"" + id
              + "\\\") { id label reference amount { value currency } "
              + "createdAt side statusInfo { status }}}\"}";
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(swanConf.getApiUrl()))
          .header("Content-Type", "application/json")
          .header("Authorization", BEARER_PREFIX + tokenManager.getSwanProjecToken())
          .POST(HttpRequest.BodyPublishers.ofString(query))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      OneTransactionResponse transactionResponse = new ObjectMapper()
          .findAndRegisterModules() //Load DateTime Module
          .readValue(response.body(), OneTransactionResponse.class);
      return Transaction.builder()
          .node(transactionResponse.getData().getTransaction())
          .build();
    } catch (IOException | URISyntaxException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
