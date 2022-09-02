package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Setter;

@Setter
public class PaymentInitiation {
  private Meta meta;
  private static final String JSON_PROPERTY_META = "meta";
  private Data data;
  private static final String JSON_PROPERTY_DATA = "data";

  @Setter
  public static class Meta {
    private String psuName;
    private static final String JSON_PROPERTY_PSU_NAME = "psu_name";

    private String psuEmail;
    private static final String JSON_PROPERTY_PSU_EMAIL = "psu_email";

    @JsonProperty(JSON_PROPERTY_PSU_NAME)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getPsuName() {
      return psuName;
    }

    @JsonProperty(JSON_PROPERTY_PSU_EMAIL)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getPsuEmail() {
      return psuEmail;
    }
  }

  @Setter
  public static class Data {
    private final String type = "payments";
    private static final String JSON_PROPERTY_TYPE = "type";
    private Attributes attributes;
    private static final String JSON_PROPERTY_ATTRIBUTES = "attributes";

    @JsonProperty(JSON_PROPERTY_TYPE)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getType() {
      return type;
    }

    @JsonProperty(JSON_PROPERTY_ATTRIBUTES)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Attributes getAttributes() {
      return attributes;
    }
  }

  @Setter
  public static class Attributes {
    private int amount;
    private static final String JSON_PROPERTY_AMOUNT = "amount";

    private final String currency = "EUR";
    private static final String JSON_PROPERTY_CURRENCY = "currency";

    private String communication;
    private static final String JSON_PROPERTY_COM = "communication";

    public Beneficiary beneficiary;
    private static final String JSON_PROPERTY_BENEFICIARY = "beneficiary";

    @JsonProperty(JSON_PROPERTY_AMOUNT)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public int getAmount() {
      return amount;
    }

    @JsonProperty(JSON_PROPERTY_CURRENCY)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCurrency() {
      return currency;
    }

    @JsonProperty(JSON_PROPERTY_COM)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCommunication() {
      return communication;
    }

    @JsonProperty(JSON_PROPERTY_BENEFICIARY)
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Beneficiary getBeneficiary() {
      return beneficiary;
    }
  }

  @JsonProperty(JSON_PROPERTY_META)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Meta getMeta() {
    return meta;
  }


  @JsonProperty(JSON_PROPERTY_META)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setMeta(Meta meta) {
    this.meta = meta;
  }

  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }


  @JsonProperty(JSON_PROPERTY_DATA)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setLabel(Data data) {
    this.data = data;
  }
}
