package app.bpartners.api.repository.swan.response;


import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.swan.model.SwanAccount;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static app.bpartners.api.model.mapper.AccountMapper.OPENED_STATUS;

@Slf4j
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
  private Data data;
  private static final String JSON_PROPERTY_DATA = "data";

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Data {
    private static final String JSON_PROPERTY_ACCOUNTS = "accounts";
    private Accounts accounts;

    @JsonProperty(JSON_PROPERTY_ACCOUNTS)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public Accounts getAccounts() {
      return accounts;
    }
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Accounts {
    private static final String JSON_PROPERTY_EDGES = "edges";
    private List<Edge> edges;

    @JsonProperty(JSON_PROPERTY_EDGES)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public List<Edge> getEdges() {
      List<Edge> edgeList = new ArrayList<>(edges);
      if (edgeList.isEmpty()) {
        throw new NotImplementedException("One user should have one active account");
      }
      Edge result = removeFirstActiveAccount(edgeList);
      boolean otherAccountsClosed = edgeList.stream()
          .noneMatch(edge -> edge.getNode().getStatusInfo().getStatus().equals(OPENED_STATUS));
      if (!otherAccountsClosed) {
        throw new NotImplementedException("One user with one active account is supported for now");
      }
      if (!edgeList.isEmpty()) {
        log.warn("Only account." + result.getNode().getId() + " is active "
            + "but following closed accounts are present : "
            + otherAccountIds(edgeList));
      }
      return List.of(result);
    }

    private Edge removeFirstActiveAccount(List<Edge> edgeList) {
      Optional<Edge> optionalActiveAccount = edgeList.stream()
          .filter(edge -> edge.getNode().getStatusInfo().getStatus().equals(OPENED_STATUS))
          .findFirst();
      if (optionalActiveAccount.isEmpty()) {
        throw new NotImplementedException("One user should have one active account");
      }
      edgeList.remove(optionalActiveAccount.get());
      return optionalActiveAccount.get();
    }

    private String otherAccountIds(List<Edge> edges) {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < edges.size(); i++) {
        builder.append("account.")
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
  @ToString
  public static class Edge {
    private static final String JSON_PROPERTY_SWAN_ACCOUNT = "node";
    private SwanAccount node;

    @JsonProperty(JSON_PROPERTY_SWAN_ACCOUNT)
    @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
    public SwanAccount getNode() {
      return node;
    }
  }

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }
}
