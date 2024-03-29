package app.bpartners.api.model;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.exception.ApiException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class Transaction {
  public static final String CREDIT_SIDE = "Credit";
  public static final String DEBIT_SIDE = "Debit";
  public static final String BOOKED_STATUS = "Booked";
  public static final String PENDING_STATUS = "Pending";
  public static final String UPCOMING_STATUS = "Upcoming";
  public static final String REJECTED_STATUS = "Rejected";
  public static final String CREDIT_SIDE_VALUE = "CREDIT_SIDE";
  public static final String DEBIT_SIDE_VALUE = "DEBIT_SIDE";
  public static final String RELEASED_STATUS = "Released";
  private String id;
  private String idAccount;
  private Long idBridge;
  private Money amount;
  private String currency;
  private String label;
  private String reference;
  private String side;
  private TransactionCategory category;
  private TransactionStatus status;
  private TransactionInvoiceDetails invoiceDetails;
  private EnableStatus enableStatus;

  public List<TransactionSupportingDocs> getSupportingDocuments() {
    // TODO: deprecated
    if (invoiceDetails != null && invoiceDetails.getFileId() != null) {
      return List.of(
          (TransactionSupportingDocs.builder()
              .fileInfo(FileInfo.builder().id(invoiceDetails.getFileId()).build())
              .build()));
    }
    return supportingDocuments;
  }

  @Getter(AccessLevel.NONE)
  private Instant paymentDatetime;

  private List<TransactionSupportingDocs> supportingDocuments = new ArrayList<>();

  public Instant getPaymentDatetime() {
    return paymentDatetime.truncatedTo(ChronoUnit.MILLIS);
  }

  public TransactionTypeEnum getType() {
    switch (side) {
      case CREDIT_SIDE:
      case CREDIT_SIDE_VALUE:
        return TransactionTypeEnum.INCOME;
      case DEBIT_SIDE:
      case DEBIT_SIDE_VALUE:
        return TransactionTypeEnum.OUTCOME;
      default:
        // TODO: add unknown side
        throw new ApiException(SERVER_EXCEPTION, "Unexpected side " + side);
    }
  }
}
