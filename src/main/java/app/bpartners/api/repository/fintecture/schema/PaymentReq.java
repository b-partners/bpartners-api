package app.bpartners.api.repository.fintecture.schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.annotations.ApiModelProperty;

@JsonPropertyOrder({
        PaymentReq.JSON_PROPERTY_META,
        PaymentReq.JSON_PROPERTY_DATA,
})
public class PaymentReq {
  public static final String JSON_PROPERTY_META = "meta";
  public Meta meta;
  public static final String JSON_PROPERTY_DATA = "data";
  public Data data;

  public static class Meta {
    public String psu_name;
    public String psu_email;
  }


  public static class Data {
    public final String type = "payments";
    public Attributes attributes;
  }

  public static class Attributes {
    public String amount;
    public final String currency = "EUR";
    public String communication;
    public Beneficiary beneficiary;
  }
  public PaymentReq meta(Meta id) {
    this.meta = meta;
    return this;
  }
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")
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

  public PaymentReq data(Data data) {
    this.data = data;
    return this;
  }
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")
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
