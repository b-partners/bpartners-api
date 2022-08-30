package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;

@Setter
public class PaymentInitiation {
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

  @Setter
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
  public static class Data {
    private String type;
    private Attributes attributes;

    @JsonProperty("type")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getType() {
      return "payments";
    }

    @JsonProperty("attributes")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Attributes getAttributes() {
      return attributes;
    }
  }

  @Setter
  public static class Attributes {
    private String currency;
    private String amount;
    private String communication;
    private Beneficiary beneficiary;

    @JsonProperty("currency")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCurrency() {
      return "EUR";
    }

    @JsonProperty("amount")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getAmount() {
      return amount;
    }

    @JsonProperty("communication")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCommunication() {
      return communication;
    }

    @JsonProperty("beneficiary")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Beneficiary getBeneficiary() {
      return beneficiary;
    }
  }
}
