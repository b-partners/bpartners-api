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
  private Meta meta;
  private Data data;

  @JsonProperty("meta")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Meta getMeta() {
    return meta;
  }

  @JsonProperty("data")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }

  @AllArgsConstructor
  @NoArgsConstructor
  @Setter
  @Builder
  public static class Meta {
    private String psuName;
    private String psuEmail;

    @JsonProperty("psu_name")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getPsuName() {
      return psuName;
    }

    @JsonProperty("psu_email")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getPsuEmail() {
      return psuEmail;
    }

  }

  @Setter
  @AllArgsConstructor
  @Builder
  @NoArgsConstructor
  public static class Data {
    private Attributes attributes;

    @JsonProperty("type")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getType() {
      return "request-to-pay";
    }

    @JsonProperty("attributes")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Attributes getAttributes() {
      return attributes;
    }
  }

  @Setter
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Attributes {
    private String amount;
    private String communication;
    //private String endToEndId;
    private Beneficiary beneficiary;

    //    @JsonProperty("end_to_end_id")
    //    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    //    public String getEndToEndId() {
    //      return endToEndId;
    //    }

    @JsonProperty("currency")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCurrency() {
      return "EUR";
    }

    @JsonProperty("scheme")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getScheme() {
      //TODO: change to SEPA or INSTANT_SEPA to force
      //Since Fintecture force SEPA or INSTANT_SEPA, it _seems_ any changes is overrided
      return "AUTO";
    }

    @JsonProperty("communication")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCommunication() {
      return communication;
    }

    @JsonProperty("amount")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getAmount() {
      return amount;
    }

    @JsonProperty("beneficiary")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Beneficiary getBeneficiary() {
      return beneficiary;
    }
  }
}
