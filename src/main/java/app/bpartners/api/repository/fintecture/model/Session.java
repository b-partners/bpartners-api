package app.bpartners.api.repository.fintecture.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Setter
@ToString
@EqualsAndHashCode
@JsonIgnoreProperties(ignoreUnknown = true)
public class Session {
  private Meta meta;
  private Data data;

  @JsonProperty("data")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Data getData() {
    return data;
  }

  @JsonProperty("meta")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Meta getMeta() {
    return meta;
  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @EqualsAndHashCode
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Meta {
    private String sessionId;
    private String customerId;
    private String code;
    private String status;

    @JsonProperty("session_id")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getSessionId() {
      return sessionId;
    }

    @JsonProperty("customer_id")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCustomerId() {
      return customerId;
    }

    @JsonProperty("code")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCode() {
      return code;
    }

    @JsonProperty("status")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getStatus() {
      return status;
    }
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  @Builder
  public static class Data {
    private String type;
    private Attributes attributes;

    @JsonProperty("attributes")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Attributes getAttributes() {
      return attributes;
    }

    @JsonProperty("type")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getType() {
      return type;
    }

  }

  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  @EqualsAndHashCode
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Attributes {
    private String amount;
    private String endToEndId;
    private String transferReason;
    private String transferState;
    private Beneficiary beneficiary;
    private String paymentScheme;

    @JsonProperty("currency")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getCurrency() {
      return "EUR";
    }

    @JsonProperty("execution_date")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public LocalDate getExecutionDate() {
      return LocalDate.now();
    }

    @JsonProperty("amount")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getAmount() {
      return amount;
    }

    @JsonProperty("end_to_end_id")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getEndToEndId() {
      return endToEndId;
    }

    @JsonProperty("transfer_reason")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getTransferReason() {
      return transferReason;
    }

    @JsonProperty("transfer_state")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getTransferState() {
      return transferState;
    }

    @JsonProperty("beneficiary")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public Beneficiary getBeneficiary() {
      return beneficiary;
    }

    @JsonProperty("payment_scheme")
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    public String getPaymentScheme() {
      return paymentScheme;
    }
  }
}
