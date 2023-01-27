package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.model.exception.BadRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.CrupdateInvoice.PaymentTypeEnum.CASH;
import static app.bpartners.api.endpoint.rest.model.CrupdateInvoice.PaymentTypeEnum.IN_INSTALMENT;

@Slf4j
@Component
@AllArgsConstructor
public class CrupdateInvoiceValidator implements Consumer<CrupdateInvoice> {
  public static final String REGION_PARIS = "Europe/Paris";
  private final LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneId.of(REGION_PARIS));
  private final PaymentRegValidator paymentValidator;
  private final CreateProductValidator createProductValidator;

  @Override
  public void accept(CrupdateInvoice invoice) {
    StringBuilder exceptionBuilder = new StringBuilder();
    //By default, invoice should have a payment type
    if (invoice.getPaymentType() == null) {
      log.warn("DEPRECATED : paymentType attribute in crupdate invoice should be mandatory."
          + "CASH type is set by default");
      invoice.setPaymentType(CASH);
    }
    if (invoice.getPaymentType() == IN_INSTALMENT
        && invoice.getPaymentRegulations() != null) {
      if (invoice.getPaymentRegulations().size() < 2) {
        exceptionBuilder.append("Multiple payments request more than one payment");
      } else {
        paymentValidator.accept(invoice.getPaymentRegulations(),
            computeTotalAmountWithVat(invoice.getProducts()));
      }
    }
    if (invoice.getStatus() == null) {
      exceptionBuilder.append("Status is mandatory. ");
    }
    if (invoice.getRef() != null && invoice.getRef().isBlank()) {
      invoice.setRef(null);
    }
    if (isBadPaymentAndSendingDate(invoice)) {
      exceptionBuilder.append("Payment date can not be after the sending date. ");
    }
    if (invoice.getPaymentType() == CASH
        && invoice.getPaymentRegulations() != null && !invoice.getPaymentRegulations().isEmpty()) {
      exceptionBuilder.append(
          "Only invoice with payment type IN_INSTALMENT handles multiple payments");
    }
    if (isBadSendingDate(invoice)) {
      log.warn("Bad sending date, actual = " + invoice.getSendingDate() + ", today=" + today);
      //TODO: uncomment if any warn message is logged anymore
      //exceptionBuilder.append("Invoice can not be sent no later than today. ");
    }

    String exceptionMessage = exceptionBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  private int computeTotalAmountWithVat(List<CreateProduct> products) {
    if (products == null || products.isEmpty()) {
      return 0;
    }
    AtomicReference<Double> amount = new AtomicReference<>(0.0);
    products.forEach(product -> {
      createProductValidator.accept(product);
      int quantity = product.getQuantity() == null
          ? 0 : product.getQuantity();
      double vat = 1 + product.getVatPercent().doubleValue() / 10000;
      int unitPrice = product.getUnitPrice() / 100;
      double priceWithVat = quantity * unitPrice * vat;
      amount.set(amount.get() + priceWithVat);
    });
    return (int) (BigDecimal.valueOf(amount.get())
        .setScale(2, RoundingMode.HALF_UP).doubleValue()
        * 100);
  }

  private boolean isBadSendingDate(CrupdateInvoice invoice) {
    return invoice.getSendingDate() != null && invoice.getSendingDate().compareTo(today) != 0
        && invoice.getSendingDate().isAfter(today);
  }

  private static boolean isBadPaymentAndSendingDate(CrupdateInvoice invoice) {
    return invoice.getToPayAt() != null && invoice.getSendingDate() != null
        && invoice.getToPayAt().isBefore(invoice.getSendingDate());
  }
}
