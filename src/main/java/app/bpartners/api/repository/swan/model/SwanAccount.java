package app.bpartners.api.repository.swan.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class SwanAccount {
  private String id;
  private String name;
  private String iban;
  private String bic;
  private Balances balances;
  private StatusInfo statusInfo;

  @JsonProperty("id")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getId() {
    return id;
  }

  @JsonProperty("name")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getName() {
    return name;
  }

  @JsonProperty("IBAN")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getIban() {
    return iban;
  }

  @JsonProperty("BIC")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getBic() {
    return bic;
  }

  @JsonProperty("balances")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Balances getBalances() {
    return balances;
  }

  @JsonProperty("statusInfo")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public StatusInfo getStatusInfo() {
    return statusInfo;
  }

  @AllArgsConstructor
  @Builder
  public static class StatusInfo {
    private String status;

    @JsonProperty("status")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getStatus() {
      return status;
    }
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Balances {
    private Available available;

    @JsonProperty("available")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Available getAvailable() {
      return available;
    }

    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Available {
      private Double value;

      @JsonProperty("value")
      @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
      public Double getValue() {
        return value;
      }
    }
  }
}
