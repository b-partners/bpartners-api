package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.repository.api.swan.SwanApi;
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import app.bpartners.api.repository.swan.response.TransactionResponse;
import app.bpartners.api.repository.swan.schema.Transaction;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionSwanRepositoryImpl implements TransactionSwanRepository {
  private static final String message =
      "{\"query\": \"query Transactions { accounts { edges { node { transactions { edges { "
          + "node { id label reference amount { value currency } createdAt } } } } } }}\"}";

  private final SwanApi<TransactionResponse> swanApi;

  @Override
  public List<Transaction> getTransactions() {
    return swanApi.getData(message, "").getData().getAccounts().getEdges().get(0).getNode()
        .getTransactions().getEdges();
  }
}
