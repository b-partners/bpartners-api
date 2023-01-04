package app.bpartners.api.repository.swan.response;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.swan.model.SwanAccountHolder;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.model.mapper.AccountHolderMapper.VERIFIED_STATUS;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountHolderResponse {
  private static final String JSON_PROPERTY_DATA = "data";
  private Data data;

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Data {
    private static final String JSON_PROPERTY_ACCOUNT_HOLDERS = "accountHolders";
    private AccountHolders accountHolders;

    @JsonProperty(JSON_PROPERTY_ACCOUNT_HOLDERS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public AccountHolders getAccountHolders() {
      return accountHolders;
    }
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class AccountHolders {
    private static final String JSON_PROPERTY_EDGES = "edges";
    private List<Edge> edges;

    @JsonProperty(JSON_PROPERTY_EDGES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public List<Edge> getEdges() {
      List<Edge> edgeList = new ArrayList<>(edges);
      if (edges.isEmpty()) {
        throw new ApiException(SERVER_EXCEPTION,
            "One account should have at least one account holder");
      }
      Edge result = removeFirstVerifiedAccount(edgeList);
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
      return List.of(result);
    }

    private Edge removeFirstVerifiedAccount(List<Edge> edgeList) {
      Optional<Edge> optionalVerified = edgeList.stream()
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

    private String accountHoldersWithId(List<Edge> edges) {
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

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Edge {
    private static final String JSON_PROPERTY_ACCOUNTHOLDER = "node";
    private SwanAccountHolder node;

    @JsonProperty(JSON_PROPERTY_ACCOUNTHOLDER)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public SwanAccountHolder getNode() {
      return node;
    }
  }
}