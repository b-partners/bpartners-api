package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.CreditOperationInvoiceCreated;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.User;
import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.service.EmailInvoiceResolver;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.mail.MessagingException;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreditOperationInvoiceCreatedServiceTest {
  private static final byte[] PDF_BYTES = new byte[] {1, 2, 3};

  InvoiceRepository invoiceRepository = mock();
  CreditPurchaseRepository creditPurchaseRepository = mock();
  S3Service s3Service = mock();
  FileWriter fileWriter = mock();
  SesService mailer = mock();
  EmailInvoiceResolver emailInvoiceResolver = mock();
  CreditOperationInvoiceCreatedService subject =
      new CreditOperationInvoiceCreatedService(
          invoiceRepository,
          creditPurchaseRepository,
          s3Service,
          fileWriter,
          mailer,
          new TemplateResolverEngine(),
          new CustomDateFormatter(),
          emailInvoiceResolver);

  CreditOperationInvoiceCreatedServiceTest() {
    when(s3Service.downloadFile(any(), anyString(), anyString()))
        .thenReturn(new File("invoice.pdf"));
    when(fileWriter.writeAsByte(any(File.class))).thenReturn(PDF_BYTES);
    when(emailInvoiceResolver.apply(any())).thenReturn("buyer@email.com");
  }

  @Test
  void sends_the_invoice_to_the_customer_with_tech_in_copy() throws Exception {
    givenInvoiceAndPurchase(someInvoice(), somePurchase());

    subject.accept(someEvent());

    var recipientCaptor = ArgumentCaptor.forClass(String.class);
    var copyCaptor = ArgumentCaptor.forClass(String.class);
    var subjectCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailer)
        .sendEmail(
            recipientCaptor.capture(),
            copyCaptor.capture(),
            subjectCaptor.capture(),
            anyString(),
            anyList());
    assertEquals("buyer@email.com", recipientCaptor.getValue());
    assertEquals("tech@birdia.fr", copyCaptor.getValue());
    assertEquals(
        "[BIRDIA] Votre facture d'achat de crédits REF-04032026103000 est disponible",
        subjectCaptor.getValue());
  }

  @Test
  void attaches_the_invoice_pdf_of_the_issuing_admin() throws Exception {
    givenInvoiceAndPurchase(someInvoice(), somePurchase());

    subject.accept(someEvent());

    verify(s3Service).downloadFile(INVOICE, "file_id", "admin_user_id");
    var attachmentsCaptor = ArgumentCaptor.forClass(List.class);
    verify(mailer)
        .sendEmail(anyString(), anyString(), anyString(), anyString(), attachmentsCaptor.capture());
    var attachment = (Attachment) attachmentsCaptor.getValue().getFirst();
    assertEquals("REF-04032026103000", attachment.getName());
    assertEquals(PDF_BYTES, attachment.getContent());
  }

  @Test
  void renders_the_purchase_summary_in_the_html_body() throws Exception {
    givenInvoiceAndPurchase(someInvoice(), somePurchase());

    subject.accept(someEvent());

    var body = capturedHtmlBody();
    assertTrue(body.contains("Buyer SARL"));
    assertTrue(body.contains("REF-04032026103000"));
    assertTrue(body.contains("Nombre de crédits"));
    assertTrue(body.contains(">30<"));
    assertTrue(body.contains("Prix unitaire HT"));
    assertTrue(body.contains("1,00 €"));
    assertTrue(body.contains("30,00 €"));
    assertTrue(body.contains("36,00 €"));
    assertTrue(body.contains("04/03/2026"));
  }

  @Test
  void does_not_repeat_the_credit_count_in_the_purchase_label() throws Exception {
    givenInvoiceAndPurchase(someInvoice(), somePurchase());

    subject.accept(someEvent());

    var body = capturedHtmlBody();
    assertFalse(body.contains("Pack 30 crédits"));
    assertFalse(body.contains("30 crédits"));
  }

  @Test
  void renders_a_modern_html_body() throws Exception {
    givenInvoiceAndPurchase(someInvoice(), somePurchase());

    subject.accept(someEvent());

    var body = capturedHtmlBody();
    assertTrue(body.contains("<style>"));
    assertTrue(body.contains("max-width: 560px"));
    assertTrue(body.contains("@media (max-width: 600px)"));
    assertTrue(body.contains("L'équipe BIRDIA"));
    assertFalse(body.contains("${"));
    assertFalse(body.contains("th:text"));
  }

  @Test
  void falls_back_on_the_invoice_line_when_the_purchase_is_gone() throws Exception {
    when(invoiceRepository.findById("invoice_id")).thenReturn(someInvoice());
    when(creditPurchaseRepository.findById("purchase_id")).thenReturn(Optional.empty());

    subject.accept(someEvent());

    var body = capturedHtmlBody();
    assertTrue(body.contains(">30<"));
    assertTrue(body.contains("1,00 €"));
  }

  @Test
  void dates_the_mail_from_the_creation_datetime_when_the_invoice_has_no_sending_date() {
    givenInvoiceAndPurchase(
        someInvoice().toBuilder()
            .sendingDate(null)
            .createdAt(Instant.parse("2026-03-04T09:30:00Z"))
            .build(),
        somePurchase());

    subject.accept(someEvent());

    assertTrue(capturedHtmlBody().contains("04/03/2026"));
  }

  @Test
  void renders_zeroed_credits_when_neither_purchase_nor_invoice_line_is_available() {
    when(invoiceRepository.findById("invoice_id"))
        .thenReturn(someInvoice().toBuilder().products(List.of()).build());
    when(creditPurchaseRepository.findById("purchase_id")).thenReturn(Optional.empty());

    assertDoesNotThrow(() -> subject.accept(someEvent()));

    var body = capturedHtmlBody();
    assertTrue(body.contains(">0<"));
    assertTrue(body.contains("0,00 €"));
  }

  @Test
  void renders_zeroed_credits_when_neither_purchase_nor_invoice_line_carries_a_quantity() {
    givenInvoiceAndPurchase(
        someInvoice().toBuilder()
            .products(
                List.of(
                    InvoiceProduct.builder()
                        .description("Pack 30 crédits")
                        .quantity(null)
                        .unitPrice(parseFraction(100))
                        .build()))
            .build(),
        somePurchase().toBuilder().credits(null).build());

    assertDoesNotThrow(() -> subject.accept(someEvent()));

    assertTrue(capturedHtmlBody().contains(">0<"));
  }

  @Test
  void renders_a_zeroed_unit_price_when_the_invoice_line_carries_no_unit_price() {
    givenInvoiceAndPurchase(
        someInvoice().toBuilder()
            .products(
                List.of(
                    InvoiceProduct.builder()
                        .description("Pack 30 crédits")
                        .quantity(30)
                        .unitPrice(null)
                        .build()))
            .build(),
        somePurchase());

    assertDoesNotThrow(() -> subject.accept(someEvent()));

    assertTrue(capturedHtmlBody().contains("0,00 €"));
  }

  @Test
  void renders_the_credit_unit_price_even_when_the_invoice_line_prices_a_whole_pack() {
    givenInvoiceAndPurchase(
        someInvoice().toBuilder()
            .products(
                List.of(
                    InvoiceProduct.builder()
                        .description("Pack de 30 crédits d'analyse")
                        .quantity(1)
                        .unitPrice(parseFraction(3000))
                        .build()))
            .build(),
        somePurchase().toBuilder().creditUnitPriceInCentsWithoutVat(100L).build());

    subject.accept(someEvent());

    var body = capturedHtmlBody();
    assertTrue(body.contains(">30<"));
    assertTrue(body.contains("1,00 €"));
  }

  @Test
  void falls_back_on_the_invoice_line_quantity_when_the_purchase_carries_no_credits() {
    givenInvoiceAndPurchase(someInvoice(), somePurchase().toBuilder().credits(null).build());

    subject.accept(someEvent());

    assertTrue(capturedHtmlBody().contains(">30<"));
  }

  @Test
  void sends_nothing_when_the_invoice_is_gone() throws Exception {
    when(invoiceRepository.findById("invoice_id")).thenReturn(null);

    subject.accept(someEvent());

    verify(mailer, never()).sendEmail(any(), any(), any(), any(), anyList());
    verify(s3Service, never()).downloadFile(any(), any(), any());
  }

  @Test
  void sends_nothing_when_no_recipient_is_resolved() throws Exception {
    givenInvoiceAndPurchase(someInvoice(), somePurchase());
    when(emailInvoiceResolver.apply(any())).thenReturn(null);

    subject.accept(someEvent());

    verify(mailer, never()).sendEmail(any(), any(), any(), any(), anyList());
    verify(s3Service, never()).downloadFile(any(), any(), any());
  }

  @Test
  void renders_without_throwing_when_the_invoice_carries_no_date() {
    givenInvoiceAndPurchase(
        someInvoice().toBuilder().sendingDate(null).createdAt(null).build(), somePurchase());

    assertDoesNotThrow(() -> subject.accept(someEvent()));
  }

  @Test
  void a_failing_mail_is_reported_as_a_server_error() throws Exception {
    givenInvoiceAndPurchase(someInvoice(), somePurchase());
    doThrow(new MessagingException("ses down"))
        .when(mailer)
        .sendEmail(anyString(), anyString(), anyString(), anyString(), anyList());

    assertThrows(ApiException.class, () -> subject.accept(someEvent()));
  }

  @Test
  void retries_the_mail_for_at_most_five_minutes() {
    var event = someEvent();

    assertEquals(Duration.ofMinutes(5L), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1L), event.maxConsumerBackoffBetweenRetries());
  }

  @SneakyThrows
  private String capturedHtmlBody() {
    var bodyCaptor = ArgumentCaptor.forClass(String.class);
    verify(mailer)
        .sendEmail(anyString(), anyString(), anyString(), bodyCaptor.capture(), anyList());
    return bodyCaptor.getValue();
  }

  private void givenInvoiceAndPurchase(Invoice invoice, CreditPurchase creditPurchase) {
    when(invoiceRepository.findById("invoice_id")).thenReturn(invoice);
    when(creditPurchaseRepository.findById("purchase_id")).thenReturn(Optional.of(creditPurchase));
  }

  private CreditOperationInvoiceCreated someEvent() {
    return CreditOperationInvoiceCreated.builder()
        .invoiceId("invoice_id")
        .creditPurchaseId("purchase_id")
        .build();
  }

  private Invoice someInvoice() {
    return Invoice.builder()
        .id("invoice_id")
        .ref("REF-04032026103000")
        .fileId("file_id")
        .user(User.builder().id("admin_user_id").build())
        .customer(
            Customer.builder()
                .id("customer_id")
                .name("Buyer SARL")
                .email("buyer@email.com")
                .build())
        .sendingDate(LocalDate.of(2026, 3, 4))
        .products(
            List.of(
                InvoiceProduct.builder()
                    .description("Pack 30 crédits")
                    .quantity(30)
                    .unitPrice(parseFraction(100))
                    .build()))
        .totalPriceWithoutVat(parseFraction(3000))
        .totalPriceWithVat(parseFraction(3600))
        .build();
  }

  private CreditPurchase somePurchase() {
    return CreditPurchase.builder()
        .id("purchase_id")
        .credits(30L)
        .creditPack(CreditPack.builder().description("Pack 30 crédits").build())
        .build();
  }
}
