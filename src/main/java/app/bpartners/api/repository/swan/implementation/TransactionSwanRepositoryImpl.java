package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import app.bpartners.api.repository.swan.model.SwanTransaction;
import app.bpartners.api.repository.swan.response.TransactionResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionSwanRepositoryImpl implements TransactionSwanRepository {
  private static final String query =
      "{\"query\": \"query Transactions { accounts { edges { node { transactions { edges { "
          + "node { id label reference amount { value currency } createdAt } } } } } }}\"}";

  private final SwanApi<TransactionResponse> swanApi;

  @Override
  public List<SwanTransaction> getTransactions() {
    try {
      return swanApi.getData(TransactionResponse.class, query, null).getData().getAccounts()
          .getEdges().get(
              0).getNode().getTransactions().getEdges();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
