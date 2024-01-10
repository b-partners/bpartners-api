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
public class FPaymentInitiation {
  @JsonProperty("meta")
  private Meta meta;

  @JsonProperty("data")
  private Data data;

  public Meta getMeta() {
    return meta;
  }

  public Data getData() {
    return data;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Setter
  @Builder
  public static class Meta {
    @JsonProperty("psu_name")
    private String psuName;

    @JsonProperty("psu_email")
    private String psuEmail;
  }

  @Setter
  @AllArgsConstructor
  @Builder
  @NoArgsConstructor
  public static class Data {
    @JsonProperty("attributes")
    private Attributes attributes;

    @JsonProperty("type")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getType() {
      return "request-to-pay";
    }
  }

  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Attributes {
    @JsonProperty("amount")
    private String amount;

    @JsonProperty("communication")
    private String communication;

    @JsonProperty("beneficiary")
    private Beneficiary beneficiary;

    @JsonProperty("currency")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCurrency() {
      return "EUR";
    }

    @JsonProperty("scheme")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getScheme() {
      return "AUTO";
    }
  }
}
