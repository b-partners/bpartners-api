package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.swan.AccountHolderSwanRepository;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.model.SwanAccountHolder;
import app.bpartners.api.repository.swan.response.AccountHolderResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.model.mapper.AccountHolderMapper.VERIFIED_STATUS;

/*Note that there is security check in security layer, so that these requests are allowed only when
 *accountId match with authenticated account_id
 */
@Slf4j
@Repository
@AllArgsConstructor
public class AccountHolderSwanRepositoryImpl implements AccountHolderSwanRepository {

  private final SwanApi<AccountHolderResponse> swanApi;
  private final SwanCustomApi<AccountHolderResponse> swanCustomApi;
  private static final String QUERY =
      "{ \"query\": \"" + "query AccountHolder { accountHolders { edges { node "
          + "{ id  info { ... on AccountHolderCompanyInfo { name registrationNumber "
          + "businessActivity businessActivityDescription }} residencyAddress "
          + "{ addressLine1 city country postalCode } verificationStatus"
          + " accounts { edges { node { id name IBAN BIC balances { available { value } } "
          + "statusInfo {status} } } }} } }}\"}";

  @Override
  public List<SwanAccountHolder> findAllByAccountId(String accountId) {
    List<AccountHolderResponse.Edge> edges =
        swanApi.getData(AccountHolderResponse.class, QUERY)
            .getData()
            .getAccountHolders()
            .getEdges();
    return getSwanAccountHolders(edges, accountId);
  }

  @Override
  public List<SwanAccountHolder> findAllByBearerAndAccountId(String bearer, String accountId) {
    List<AccountHolderResponse.Edge> edges =
        swanCustomApi.getData(AccountHolderResponse.class, QUERY, bearer)
            .getData()
            .getAccountHolders()
            .getEdges();
    return getSwanAccountHolders(edges, accountId);
  }

  @Override
  public SwanAccountHolder getById(String id) {
    SwanAccountHolder accountHolder =
        swanApi.getData(AccountHolderResponse.class, QUERY).getData().getAccountHolders()
            .getEdges().get(0).getNode();
    if (!accountHolder.getId().equals(id)) {
      throw new NotFoundException("AccountHolder." + id + " not found");
    }
    return accountHolder;
  }

  private List<SwanAccountHolder> getSwanAccountHolders(List<AccountHolderResponse.Edge> edges,
                                                        String accountId) {
    List<AccountHolderResponse.Edge> filteredByAccount = edges.stream()
        .filter(edge ->
            edge.getNode().getAccounts().getEdges().get(0).getNode().getId().equals(accountId))
        .collect(Collectors.toUnmodifiableList());
    List<AccountHolderResponse.Edge> edgeList = new ArrayList<>(filteredByAccount);
    if (edges.isEmpty()) {
      throw new ApiException(SERVER_EXCEPTION,
          "One account should have at least one account holder");
    }
    AccountHolderResponse.Edge result = removeFirstVerifiedAccount(edgeList);
    boolean otherAccountsNotVerified = edgeList.stream()
        .noneMatch(edge -> edge.getNode().getVerificationStatus().equals(VERIFIED_STATUS));
    if (!otherAccountsNotVerified
        && result.getNode().getVerificationStatus().equals(VERIFIED_STATUS)) {
      throw new NotImplementedException(
          "One account with one verified account holder is supported for now"
              + " but following verified account holders are found : "
              + accountHoldersWithId(edgeList.stream()
              .filter(edge -> edge.getNode().getVerificationStatus().equals(VERIFIED_STATUS))
              .collect(Collectors.toUnmodifiableList())));
    }
    if (!edgeList.isEmpty()) {
      log.warn("Only accountHolder." + result.getNode().getId() + " is verified "
          + "but following unverified accountHolders are present : "
          + accountHoldersWithId(edgeList));
    }
    return List.of(result.getNode());
  }

  private AccountHolderResponse.Edge removeFirstVerifiedAccount(
      List<AccountHolderResponse.Edge> edgeList) {
    Optional<AccountHolderResponse.Edge> optionalVerified = edgeList.stream()
        .filter(edge -> edge.getNode().getVerificationStatus().equals(VERIFIED_STATUS))
        .findFirst();
    if (optionalVerified.isEmpty()) {
      if (edgeList.size() == 1) {
        return edgeList.get(0);
      } else {
        throw new NotImplementedException("Only one unverified account holder is supported for "
            + "now but following unverified accountHolders are present : "
            + accountHoldersWithId(edgeList));
      }
    }
    edgeList.remove(optionalVerified.get());
    return optionalVerified.get();
  }

  private String accountHoldersWithId(List<AccountHolderResponse.Edge> edges) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < edges.size(); i++) {
      builder.append("accountHolder.")
          .append(edges.get(i).getNode().getId());
      if (i != edges.size() - 1) {
        builder.append(", ");
      }
    }
    return builder.toString();
  }
}
