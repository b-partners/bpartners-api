package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import app.bpartners.api.repository.swan.model.Transaction;
import app.bpartners.api.repository.swan.response.TransactionResponse;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionSwanRepositoryImpl implements TransactionSwanRepository {

  private static final String QUERY =
      "{\"query\": \"query Transactions { accounts { edges { node { transactions { edges { "
          + "node { id label reference amount { value currency } createdAt } } } } } }}\"}";

  private final SwanApi<TransactionResponse> swanApi;

  @Override
  public List<Transaction> getTransactions() {
    return swanApi.getData(TransactionResponse.class, QUERY).getData().getAccounts().getEdges().get(
        0).getNode().getTransactions().getEdges();
  }

}
