package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import static app.bpartners.api.service.InvoiceService.DRAFT_REF_PREFIX;
import static app.bpartners.api.service.InvoiceService.PROPOSAL_REF_PREFIX;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Invoice {
  public static final int DEFAULT_TO_PAY_DELAY_DAYS = 30;
  public static final int DEFAULT_DELAY_PENALTY_PERCENT = 0;
  private String id;
  private PaymentTypeEnum paymentType;
  private String title;
  private String ref;
  private LocalDate sendingDate;
  private LocalDate validityDate;
  private LocalDate toPayAt;
  private Integer delayInPaymentAllowed;
  private Fraction delayPenaltyPercent;
  private Fraction totalPriceWithoutDiscount;
  private Fraction totalPriceWithoutVat;
  private Fraction totalVat;
  private Fraction totalPriceWithVat;
  private String paymentUrl;

  private Customer customer;
  private String customerEmail;
  private String customerPhone;
  private String customerAddress;
  private String customerWebsite;
  private String customerCity;
  private Integer customerZipCode;
  private String customerCountry;

  private Account account;
  private List<InvoiceProduct> products;
  private InvoiceStatus status;
  private String comment;
  private Instant updatedAt;
  private String fileId;
  private boolean toBeRelaunched;
  private Instant createdAt;
  private Map<String, String> metadata;
  private List<CreatePaymentRegulation> multiplePayments;

  public Invoice multiplePayments(List<CreatePaymentRegulation> multiplePayments) {
    this.multiplePayments = multiplePayments;
    return this;
  }

  private InvoiceDiscount discount;

  public String getRealReference() {
    if (getRef() == null) {
      return null;
    }
    if (getRef().contains(DRAFT_REF_PREFIX)) {
      return getRef().replace(DRAFT_REF_PREFIX, "");
    } else if (getRef().contains(PROPOSAL_REF_PREFIX)) {
      return getRef().replace(PROPOSAL_REF_PREFIX, "");
    }
    return getRef();
  }

  public Date getFormattedSendingDate() {
    if (sendingDate == null) {
      return null;
    }
    return Date.from(sendingDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  public Date getFormattedPayingDate() {
    if (toPayAt == null) {
      return null;
    }
    return Date.from(toPayAt.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  public Date getFormattedValidityDate() {
    if (validityDate == null) {
      return null;
    }
    return Date.from(validityDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
  }

  public Integer getDelayInPaymentAllowed() {
    if (delayInPaymentAllowed == null) {
      return null;
    }
    return delayInPaymentAllowed;
  }

  public Fraction getDelayPenaltyPercent() {
    if (delayPenaltyPercent == null) {
      return parseFraction(DEFAULT_DELAY_PENALTY_PERCENT);
    }
    return delayPenaltyPercent;
  }

  public List<CreatePaymentRegulation> getSortedMultiplePayments() {
    return multiplePayments.stream()
        .sorted(Comparator.comparing(CreatePaymentRegulation::getMaturityDate))
        .collect(Collectors.toList());
  }
}
