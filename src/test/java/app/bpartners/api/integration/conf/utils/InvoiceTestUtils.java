package app.bpartners.api.integration.conf.utils;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.CrupdateInvoice.PaymentTypeEnum.IN_INSTALMENT;
import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.CASH;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.PaymentMethod.BANK_TRANSFER;
import static app.bpartners.api.endpoint.rest.model.PaymentMethod.CHEQUE;
import static app.bpartners.api.endpoint.rest.model.PaymentMethod.UNKNOWN;
import static app.bpartners.api.endpoint.rest.model.PaymentStatus.UNPAID;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.createProduct4;
import static app.bpartners.api.integration.conf.utils.TestUtils.createProduct5;
import static app.bpartners.api.integration.conf.utils.TestUtils.customer1;
import static app.bpartners.api.integration.conf.utils.TestUtils.datedPaymentRequest1;
import static app.bpartners.api.integration.conf.utils.TestUtils.datedPaymentRequest2;
import static app.bpartners.api.integration.conf.utils.TestUtils.product3;
import static app.bpartners.api.integration.conf.utils.TestUtils.product4;
import static app.bpartners.api.integration.conf.utils.TestUtils.product5;
import static app.bpartners.api.model.Invoice.DEFAULT_DELAY_PENALTY_PERCENT;
import static app.bpartners.api.model.Invoice.DEFAULT_TO_PAY_DELAY_DAYS;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.model.CreatePaymentRegulation;
import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.InvoiceDiscount;
import app.bpartners.api.endpoint.rest.model.InvoicePaymentReq;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.endpoint.rest.model.PaymentRegStatus;
import app.bpartners.api.endpoint.rest.model.PaymentRegulation;
import app.bpartners.api.endpoint.rest.model.Product;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.junit.jupiter.api.function.Executable;

public class InvoiceTestUtils {
  public static final String DRAFT_REF_PREFIX = "BROUILLON-";
  public static final String NEW_INVOICE_ID = "invoice_uuid";

  public static Executable payment_reg_amount_less_than_100_percent_exec(
      PayingApi api, List<CreateProduct> products) {
    return () ->
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            String.valueOf(randomUUID()),
            new CrupdateInvoice()
                .status(DRAFT)
                .paymentType(IN_INSTALMENT)
                .products(products)
                .paymentRegulations(
                    List.of(
                        new CreatePaymentRegulation()
                            .amount(10)
                            .percent(null)
                            .maturityDate(LocalDate.now()),
                        new CreatePaymentRegulation()
                            .amount(10)
                            .percent(null)
                            .maturityDate(LocalDate.now()))));
  }

  public static Executable payment_reg_percent_less_than_100_percent_exec(
      PayingApi api, List<CreateProduct> products) {
    return () ->
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            String.valueOf(randomUUID()),
            new CrupdateInvoice()
                .status(DRAFT)
                .paymentType(IN_INSTALMENT)
                .products(products)
                .paymentRegulations(
                    List.of(
                        new CreatePaymentRegulation()
                            .amount(null)
                            .percent(512)
                            .maturityDate(LocalDate.now()),
                        new CreatePaymentRegulation()
                            .amount(null)
                            .percent(9000)
                            .maturityDate(LocalDate.now()))));
  }

  public static Executable payment_reg_percent_higher_than_100_percent_exec(
      PayingApi api, List<CreateProduct> products) {
    return () ->
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            String.valueOf(randomUUID()),
            new CrupdateInvoice()
                .status(DRAFT)
                .paymentType(IN_INSTALMENT)
                .products(products)
                .paymentRegulations(
                    List.of(
                        new CreatePaymentRegulation()
                            .amount(null)
                            .percent(2000)
                            .maturityDate(LocalDate.now()),
                        new CreatePaymentRegulation()
                            .amount(null)
                            .percent(9000)
                            .maturityDate(LocalDate.now()))));
  }

  public static Executable payment_reg_amount_higher_than_100_percent_exec(
      PayingApi api, List<CreateProduct> products) {
    return () ->
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            String.valueOf(randomUUID()),
            new CrupdateInvoice()
                .status(DRAFT)
                .paymentType(IN_INSTALMENT)
                .products(products)
                .paymentRegulations(
                    List.of(
                        new CreatePaymentRegulation().amount(261).maturityDate(LocalDate.now()),
                        new CreatePaymentRegulation().amount(60).maturityDate(LocalDate.now()))));
  }

  public static Executable payment_reg_more_than_one_payment_exec(PayingApi api) {
    return () ->
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            String.valueOf(randomUUID()),
            new CrupdateInvoice()
                .status(DRAFT)
                .paymentType(IN_INSTALMENT)
                .paymentRegulations(List.of(new CreatePaymentRegulation())));
  }

  public static Executable discount_percent_excedeed_exec(PayingApi api) {
    return () ->
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            randomUUID().toString(),
            validInvoice()
                .globalDiscount(new InvoiceDiscount().percentValue(12000).amountValue(null)));
  }

  public static Executable discount_amount_not_supported_exec(PayingApi api) {
    return () ->
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID,
            randomUUID().toString(),
            validInvoice().globalDiscount(new InvoiceDiscount().amountValue(0).percentValue(null)));
  }

  public static Executable non_existent_customer_exec(
      PayingApi api, CrupdateInvoice crupdateInvoiceWithNonExistentCustomer) {
    return () ->
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID, NEW_INVOICE_ID, crupdateInvoiceWithNonExistentCustomer);
  }

  public static Executable unique_ref_violation_exec(PayingApi api, String uniqueRef) {
    return () ->
        api.crupdateInvoice(
            JOE_DOE_ACCOUNT_ID, randomUUID().toString(), validInvoice().ref(uniqueRef));
  }

  public static Executable first_ref_exec(PayingApi api, String firstInvoiceId, String uniqueRef) {
    return () ->
        api.crupdateInvoice(JOE_DOE_ACCOUNT_ID, firstInvoiceId, validInvoice().ref(uniqueRef));
  }

  private static PaymentRegulation expectedDated2() {
    return new PaymentRegulation()
        .status(new PaymentRegStatus().paymentStatus(UNPAID))
        .maturityDate(LocalDate.of(2023, 2, 15))
        .paymentRequest(
            new InvoicePaymentReq()
                .reference("BP005")
                .payerName(customer1().getFirstName())
                .payerEmail(customer1().getEmail())
                .paymentUrl("https://connect-v2-sbx.fintecture.com")
                .percentValue(10000 - 909)
                .amount(1000)
                .comment("Montant restant")
                .label("Facture achat - Restant dû")
                .paymentStatus(UNPAID));
  }

  private static PaymentRegulation expectedDated1() {
    return new PaymentRegulation()
        .status(new PaymentRegStatus().paymentStatus(UNPAID))
        .maturityDate(LocalDate.of(2023, 2, 1))
        .paymentRequest(
            new InvoicePaymentReq()
                .reference("BP005")
                .payerName(customer1().getFirstName())
                .payerEmail(customer1().getEmail())
                .paymentUrl("https://connect-v2-sbx.fintecture.com")
                .percentValue(909)
                .amount(100)
                .comment("Un euro")
                .label("Facture achat - Acompte N°1")
                .paymentStatus(UNPAID));
  }

  public static List<PaymentRegulation> ignoreIdsAndDatetime(Invoice actualConfirmed) {
    List<PaymentRegulation> paymentRegulations =
        actualConfirmed.getPaymentRegulations().stream()
            .map(
                datedPaymentRequest -> {
                  datedPaymentRequest.getPaymentRequest().id(null).initiatedDatetime(null);
                  datedPaymentRequest.getStatus().updatedAt(null);
                  return datedPaymentRequest;
                })
            .collect(Collectors.toList());
    return paymentRegulations;
  }

  public static List<PaymentRegulation> ignoreIdsAndDatetimeAndUrl(Invoice actualConfirmed) {
    List<PaymentRegulation> paymentRegulations =
        new ArrayList<>(actualConfirmed.getPaymentRegulations());
    paymentRegulations.forEach(
        paymentRegulation -> {
          paymentRegulation.setPaymentRequest(
              paymentRegulation
                  .getPaymentRequest()
                  .id(null)
                  .paymentUrl(null)
                  .initiatedDatetime(null));
          paymentRegulation.getStatus().setUpdatedAt(null); // TODO: must no be ignored
        });

    ignoreSeconds(paymentRegulations);
    return paymentRegulations;
  }

  public static List<PaymentRegulation> ignoreStatusDatetime(Invoice actualConfirmed) {
    List<PaymentRegulation> paymentRegulations = actualConfirmed.getPaymentRegulations();
    paymentRegulations.forEach(
        paymentRegulation ->
            paymentRegulation.setStatus(paymentRegulation.getStatus().updatedAt(null)));
    return paymentRegulations;
  }

  public static Customer ignoreCustomerDatetime(Invoice actualConfirmed) {
    Customer actualCustomer = actualConfirmed.getCustomer();
    actualCustomer.setUpdatedAt(null);
    actualCustomer.setCreatedAt(null);
    return actualCustomer;
  }

  public static List<PaymentRegulation> ignoreSeconds(List<PaymentRegulation> paymentRegulations) {
    paymentRegulations.forEach(
        datedPaymentRequest -> {
          Instant updatedAt =
              Objects.requireNonNull(datedPaymentRequest.getStatus()).getUpdatedAt();
          datedPaymentRequest.setStatus(
              datedPaymentRequest
                  .getStatus()
                  .updatedAt(updatedAt == null ? null : updatedAt.truncatedTo(ChronoUnit.MINUTES)));
        });
    return paymentRegulations;
  }

  public static CrupdateInvoice confirmedInvoice() {
    return new CrupdateInvoice()
        .ref("BP005")
        .title("Facture achat")
        .customer(customer1())
        .products(List.of(createProduct5()))
        .paymentRegulations(
            List.of(
                new CreatePaymentRegulation()
                    .maturityDate(LocalDate.of(2023, 2, 1))
                    .amount(100)
                    .percent(null)
                    .comment("Un euro"),
                new CreatePaymentRegulation()
                    .maturityDate(LocalDate.of(2023, 2, 15))
                    .amount(1000)
                    .percent(null)
                    .comment("Montant restant")))
        .paymentType(IN_INSTALMENT)
        .status(CONFIRMED)
        .sendingDate(LocalDate.of(2022, 10, 12))
        .validityDate(LocalDate.of(2022, 10, 14))
        .toPayAt(LocalDate.of(2022, 11, 13))
        .delayInPaymentAllowed(15)
        .delayPenaltyPercent(20)
        .paymentMethod(CHEQUE);
  }

  public static CrupdateInvoice paidInvoice() {
    return new CrupdateInvoice()
        .ref("BP005")
        .title("Facture achat")
        .customer(customer1())
        .products(List.of(createProduct5()))
        .paymentType(IN_INSTALMENT)
        .status(PAID)
        .sendingDate(LocalDate.of(2022, 10, 12))
        .validityDate(LocalDate.of(2022, 10, 14))
        .toPayAt(LocalDate.of(2022, 11, 13))
        .delayInPaymentAllowed(15)
        .paymentMethod(BANK_TRANSFER)
        .paymentRegulations(
            List.of(
                new CreatePaymentRegulation()
                    .amount(100)
                    .percent(null)
                    .maturityDate(LocalDate.now()),
                new CreatePaymentRegulation()
                    .amount(1000)
                    .percent(null)
                    .maturityDate(LocalDate.now())))
        .delayPenaltyPercent(20);
  }

  public static Invoice invoice1() {
    return new Invoice()
        .id(INVOICE1_ID)
        .fileId("file1_id")
        .comment(null)
        .title("Outils pour plomberie")
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .paymentType(Invoice.PaymentTypeEnum.IN_INSTALMENT)
        .paymentRegulations(List.of(datedPaymentRequest1(), datedPaymentRequest2()))
        .customer(customer1())
        .ref("BP001")
        .createdAt(Instant.parse("2022-01-01T01:00:00.00Z"))
        .sendingDate(LocalDate.of(2022, 9, 1))
        .validityDate(LocalDate.of(2022, 10, 3))
        .toPayAt(LocalDate.of(2022, 10, 1))
        .delayInPaymentAllowed(null)
        .delayPenaltyPercent(0)
        .status(CONFIRMED)
        .archiveStatus(ENABLED)
        .products(List.of(product3(), product4()))
        .totalPriceWithVat(8800)
        .totalVat(800)
        .totalPriceWithoutVat(8000)
        .totalPriceWithoutDiscount(8000)
        .globalDiscount(new InvoiceDiscount().amountValue(0).percentValue(0))
        .paymentMethod(UNKNOWN)
        .metadata(Map.of());
  }

  public static Invoice expectedMultiplePayments(String id, Invoice actualConfirmed) {
    return new Invoice()
        .id(actualConfirmed.getId())
        .title(actualConfirmed.getTitle())
        .ref(actualConfirmed.getRef())
        .archiveStatus(actualConfirmed.getArchiveStatus())
        .paymentType(actualConfirmed.getPaymentType())
        .createdAt(actualConfirmed.getCreatedAt())
        .updatedAt(actualConfirmed.getUpdatedAt())
        .fileId(actualConfirmed.getFileId())
        .products(List.of(product4().id(null)))
        .totalVat(actualConfirmed.getTotalVat())
        .status(actualConfirmed.getStatus())
        .metadata(actualConfirmed.getMetadata())
        .toPayAt(actualConfirmed.getToPayAt())
        .sendingDate(actualConfirmed.getSendingDate())
        .totalPriceWithoutDiscount(2000)
        .totalPriceWithVat(actualConfirmed.getTotalPriceWithVat())
        .totalPriceWithoutVat(actualConfirmed.getTotalPriceWithoutVat())
        .customer(actualConfirmed.getCustomer())
        .delayPenaltyPercent(actualConfirmed.getDelayPenaltyPercent())
        .delayInPaymentAllowed(actualConfirmed.getDelayInPaymentAllowed())
        .paymentUrl(actualConfirmed.getPaymentUrl())
        .paymentMethod(UNKNOWN)
        .globalDiscount(new InvoiceDiscount().amountValue(0).percentValue(0))
        .paymentRegulations(confirmedPaymentRegulations(id));
  }

  public static CrupdateInvoice validInvoice() {
    return initializeDraft()
        .ref("BP003")
        .title("Facture sans produit")
        .comment("Nouveau commentaire")
        .customer(customer1())
        .products(List.of(createProduct4(), createProduct5()))
        .sendingDate(LocalDate.now())
        .validityDate(LocalDate.now().plusDays(3L))
        .globalDiscount(new InvoiceDiscount().amountValue(null).percentValue(1000))
        .delayInPaymentAllowed(null)
        .paymentMethod(PaymentMethod.CASH)
        .delayPenaltyPercent(null);
  }

  public static Invoice expectedDraft() {
    return new Invoice()
        .id(NEW_INVOICE_ID)
        .comment(validInvoice().getComment())
        .ref(DRAFT_REF_PREFIX + validInvoice().getRef())
        .title("Facture sans produit")
        .customer(validInvoice().getCustomer())
        .status(DRAFT)
        .sendingDate(validInvoice().getSendingDate())
        .validityDate(validInvoice().getValidityDate())
        .delayInPaymentAllowed(null)
        .delayPenaltyPercent(DEFAULT_DELAY_PENALTY_PERCENT)
        .products(
            List.of(
                product4().id(null).totalVat(180).totalPriceWithVat(1980),
                product5().id(null).totalVat(90).totalPriceWithVat(990)))
        .totalPriceWithoutDiscount(3000)
        .totalPriceWithoutVat(1800 + 900) // with discount without vat
        .totalVat(180 + 90)
        .totalPriceWithVat(1980 + 990) // or 2700 + 270 of vat
        .globalDiscount(new InvoiceDiscount().amountValue(300).percentValue(1000))
        .paymentRegulations(List.of())
        .paymentType(CASH)
        .paymentMethod(PaymentMethod.CASH)
        .metadata(Map.of());
  }

  public static Invoice expectedConfirmed() {
    return new Invoice()
        .paymentUrl(null)
        .ref(confirmedInvoice().getRef())
        .title(confirmedInvoice().getTitle())
        .customer(confirmedInvoice().getCustomer())
        .status(CONFIRMED)
        .archiveStatus(ENABLED)
        .sendingDate(confirmedInvoice().getSendingDate())
        .products(List.of(product5().id(null)))
        .paymentRegulations(List.of(expectedDated1(), expectedDated2()))
        .paymentType(Invoice.PaymentTypeEnum.IN_INSTALMENT)
        .toPayAt(LocalDate.of(2022, 11, 13))
        .delayInPaymentAllowed(confirmedInvoice().getDelayInPaymentAllowed())
        .delayPenaltyPercent(confirmedInvoice().getDelayPenaltyPercent())
        .totalPriceWithVat(1100)
        .totalVat(100)
        .totalPriceWithoutVat(1000)
        .totalPriceWithoutDiscount(1000)
        .globalDiscount(new InvoiceDiscount().amountValue(0).percentValue(0))
        .paymentMethod(CHEQUE)
        .metadata(Map.of());
  }

  public static Invoice expectedInitializedDraft() {
    return new Invoice()
        .id(NEW_INVOICE_ID)
        .products(List.of())
        .paymentRegulations(List.of())
        .paymentType(CASH)
        .totalVat(0)
        .totalPriceWithoutVat(0)
        .totalPriceWithoutDiscount(0)
        .totalPriceWithVat(0)
        .status(DRAFT)
        .archiveStatus(ENABLED)
        .delayInPaymentAllowed(DEFAULT_TO_PAY_DELAY_DAYS)
        .delayPenaltyPercent(DEFAULT_DELAY_PENALTY_PERCENT)
        .globalDiscount(new InvoiceDiscount().percentValue(0).amountValue(0))
        .paymentMethod(UNKNOWN)
        .metadata(Map.of());
  }

  public static Invoice expectedPaid() {
    return new Invoice()
        .paymentUrl("https://connect-v2-sbx.fintecture.com")
        .ref(paidInvoice().getRef())
        .title(paidInvoice().getTitle())
        .customer(paidInvoice().getCustomer())
        .status(PAID)
        .archiveStatus(ENABLED)
        .sendingDate(paidInvoice().getSendingDate())
        .products(List.of(product5().id(null)))
        .paymentType(Invoice.PaymentTypeEnum.IN_INSTALMENT)
        .toPayAt(paidInvoice().getToPayAt())
        .delayInPaymentAllowed(paidInvoice().getDelayInPaymentAllowed())
        .delayPenaltyPercent(paidInvoice().getDelayPenaltyPercent())
        .totalPriceWithVat(1100)
        .totalVat(100)
        .totalPriceWithoutVat(1000)
        .totalPriceWithoutDiscount(1000)
        .metadata(Map.of())
        .paymentMethod(BANK_TRANSFER)
        .globalDiscount(new InvoiceDiscount().amountValue(0).percentValue(0));
  }

  public static List<PaymentRegulation> initPaymentReg(String id) {
    return List.of(
        new PaymentRegulation()
            .status(new PaymentRegStatus().paymentStatus(UNPAID))
            .maturityDate(LocalDate.of(2023, 1, 1))
            .paymentRequest(
                new InvoicePaymentReq()
                    .paymentUrl(null)
                    .reference(id)
                    .percentValue(2510)
                    .amount(552)
                    .payerName("Luc")
                    .payerEmail("bpartners.artisans@gmail.com")
                    .comment("Acompte de 10%")
                    .label("Fabrication Jean" + " - Acompte N°1")
                    .paymentStatus(UNPAID)),
        new PaymentRegulation()
            .status(new PaymentRegStatus().paymentStatus(UNPAID))
            .maturityDate(LocalDate.of(2023, 1, 1))
            .paymentRequest(
                new InvoicePaymentReq()
                    .paymentUrl(null)
                    .percentValue(10000 - 2510)
                    .amount(1648)
                    .reference(id)
                    .payerName("Luc")
                    .payerEmail("bpartners.artisans@gmail.com")
                    .comment("Reste 90%")
                    .label("Fabrication Jean" + " - Restant dû")
                    .paymentStatus(UNPAID)));
  }

  public static List<PaymentRegulation> updatedPaymentRegulations(String id) {
    return List.of(
        new PaymentRegulation()
            .status(new PaymentRegStatus().paymentStatus(UNPAID))
            .maturityDate(LocalDate.of(2023, 1, 1))
            .paymentRequest(
                new InvoicePaymentReq()
                    .paymentUrl(null)
                    .reference(id)
                    .percentValue(1025)
                    .amount(225)
                    .payerName("Luc")
                    .payerEmail("bpartners.artisans@gmail.com")
                    .comment("Acompte de 10%")
                    .label("Fabrication Jean" + " - Acompte N°1")
                    .paymentStatus(UNPAID)),
        new PaymentRegulation()
            .status(new PaymentRegStatus().paymentStatus(UNPAID))
            .maturityDate(LocalDate.of(2023, 1, 1))
            .paymentRequest(
                new InvoicePaymentReq()
                    .paymentUrl(null)
                    .percentValue(10000 - 1025)
                    .amount(1975)
                    .reference(id)
                    .payerName("Luc")
                    .comment("Reste 90%")
                    .payerEmail("bpartners.artisans@gmail.com")
                    .label("Fabrication Jean" + " - Restant dû")
                    .paymentStatus(UNPAID)));
  }

  public static List<PaymentRegulation> confirmedPaymentRegulations(String id) {
    return List.of(
        new PaymentRegulation()
            .status(new PaymentRegStatus().paymentStatus(UNPAID))
            .maturityDate(LocalDate.of(2023, 1, 1))
            .paymentRequest(
                new InvoicePaymentReq()
                    .paymentUrl("https://connect-v2-sbx.fintecture.com")
                    .reference(id)
                    .percentValue(1025)
                    .amount(225)
                    .payerName("Luc")
                    .comment("Acompte de 10%")
                    .payerEmail("bpartners.artisans@gmail.com")
                    .label("Fabrication Jean" + " - Acompte N°1")
                    .paymentStatus(UNPAID)),
        new PaymentRegulation()
            .status(new PaymentRegStatus().paymentStatus(UNPAID))
            .maturityDate(LocalDate.of(2023, 1, 1))
            .paymentRequest(
                new InvoicePaymentReq()
                    .paymentUrl("https://connect-v2-sbx.fintecture.com")
                    .percentValue(10000 - 1025)
                    .amount(1975)
                    .reference(id)
                    .payerName("Luc")
                    .payerEmail("bpartners.artisans@gmail.com")
                    .comment("Reste 90%")
                    .label("Fabrication Jean" + " - Restant dû")
                    .paymentStatus(UNPAID)));
  }

  public static List<Product> ignoreIdsOf(List<Product> actual) {
    return actual.stream().peek(product -> product.setId(null)).toList();
  }

  public static List<Invoice> ignoreUpdatedAt(List<Invoice> actual) {
    actual.forEach(
        invoice -> {
          invoice.setUpdatedAt(null);
        });
    return actual;
  }

  public static CrupdateInvoice initializeDraft() {
    return new CrupdateInvoice().status(DRAFT);
  }

  public static Invoice invoice6() {
    return new Invoice()
        .id("invoice6_id")
        .paymentUrl(null)
        .comment(null)
        .ref(DRAFT_REF_PREFIX + "BP007")
        .title("Facture transaction")
        .customer(customer1())
        .status(DRAFT)
        .archiveStatus(ENABLED)
        .createdAt(Instant.parse("2022-01-01T06:00:00Z"))
        .sendingDate(LocalDate.of(2022, 10, 12))
        .validityDate(LocalDate.of(2022, 11, 12))
        .delayInPaymentAllowed(null)
        .delayPenaltyPercent(0)
        .paymentRegulations(List.of())
        .paymentType(CASH)
        .toPayAt(LocalDate.of(2022, 11, 10))
        .products(List.of())
        .totalPriceWithVat(0)
        .totalPriceWithoutVat(0)
        .totalPriceWithoutDiscount(0)
        .totalVat(0)
        .globalDiscount(new InvoiceDiscount().amountValue(0).percentValue(0))
        .paymentMethod(UNKNOWN)
        .metadata(Map.of());
  }
}
