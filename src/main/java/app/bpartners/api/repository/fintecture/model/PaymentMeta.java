package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
public class PaymentMeta {
  private Meta meta;

  @JsonProperty("meta")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Meta getMeta() {
    return meta;
  }

  @Setter
  public static class Meta {
    private String status;
    private String transferReason;
    private String reason;

    @JsonProperty("status")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getStatus() {
      return status;
    }

    @JsonProperty("origin")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getOrigin() {
      return "api";
    }

    @JsonProperty("transfer_reason")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getTransferReason() {
      return transferReason;
    }

    @JsonProperty("reason")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getReason() {
      return reason;
    }
  }
}
