package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.IN_INSTALMENT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.integration.conf.utils.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import app.bpartners.api.endpoint.rest.model.CustomerType;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.PaymentHistoryStatus;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.User;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.InvoicePdfUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Disabled
class DraftIT extends MockedThirdParties {
  @Autowired private SesService subject;

  private static void generatePdf(String templateName) throws IOException {
    List<CreatePaymentRegulation> paymentRegulations =
        List.of(
            CreatePaymentRegulation.builder()
                .paymentRequest(
                    PaymentRequest.builder()
                        .paymentHistoryStatus(
                            PaymentHistoryStatus.builder()
                                .status(PaymentStatus.PAID)
                                .paymentMethod(PaymentMethod.CASH)
                                .userUpdated(true)
                                .updatedAt(Instant.now())
                                .build())
                        .amount(new Fraction(BigInteger.valueOf(100)))
                        .paymentUrl("https://connect-v2-sbx.fintecture.com")
                        .build())
                .maturityDate(LocalDate.now().plusDays(1L))
                .comment(null)
                .build(),
            CreatePaymentRegulation.builder()
                .paymentRequest(
                    PaymentRequest.builder()
                        .paymentHistoryStatus(
                            PaymentHistoryStatus.builder().status(PaymentStatus.UNPAID).build())
                        .amount(new Fraction(BigInteger.TEN))
                        .paymentUrl("https://connect-v2-sbx.fintecture.com")
                        .build())
                .comment("Avec un assez long commentaire pour voir si ça descend automatiquement")
                .maturityDate(LocalDate.now())
                .build());
    app.bpartners.api.model.Invoice invoice =
        Invoice.builder()
            .id(INVOICE1_ID)
            .ref("invoice_ref")
            .title("invoice_title")
            .status(CONFIRMED)
            .paymentMethod(PaymentMethod.UNKNOWN)
            .sendingDate(LocalDate.now())
            .toPayAt(LocalDate.now())
            .delayInPaymentAllowed(2)
            .delayPenaltyPercent(new Fraction(BigInteger.TEN, BigInteger.ONE))
            .totalPriceWithoutVat(new Fraction())
            .totalVat(new Fraction())
            .totalPriceWithoutDiscount(new Fraction())
            .totalPriceWithVat(new Fraction())
            .discount(
                new InvoiceDiscount()
                    .toBuilder().percentValue(new Fraction()).amountValue(new Fraction()).build())
            .user(
                User.builder()
                    .id(JOE_DOE_ID)
                    .accounts(
                        List.of(
                            Account.builder()
                                .id(JOE_DOE_ACCOUNT_ID)
                                .name("BPartners")
                                .iban("FR7630001007941234567890185")
                                .bic("BPFRPP751")
                                .build()))
                    .build())
            .paymentType(IN_INSTALMENT)
            .paymentRegulations(paymentRegulations)
            .products(creatableProds(1))
            .customer(
                Customer.builder()
                    .name("Must be not shown")
                    .firstName("Olivier")
                    .lastName("Durant")
                    .phone("+33 6 12 45 89 76")
                    .email("exemple@email.com")
                    .address("Paris 745")
                    .customerType(CustomerType.PROFESSIONAL)
                    .build())
            .paymentUrl("text")
            .build();
    InvoicePdfUtils pdfUtils = new InvoicePdfUtils();
    byte[] logoAsBytes =
        new ClassPathResource("files/downloaded.jpeg").getInputStream().readAllBytes();
    AccountHolder accountHolder =
        AccountHolder.builder()
            .name("Numer")
            .mobilePhoneNumber("06 12 34 56 78")
            .email("numer@hei.school")
            .siren("9120384183")
            .vatNumber("FR2938410231")
            .socialCapital(10000)
            .subjectToVat(true)
            .build();
    byte[] data = pdfUtils.generatePdf(invoice, accountHolder, logoAsBytes, templateName);
    File generatedFile = new File(randomUUID() + ".pdf");
    OutputStream os = new FileOutputStream(generatedFile);
    os.write(data);
    os.close();
  }

  /*@Test
  void find_legal_files_ok() {
    List<LegalFile> actual = legalFileRepository.findAllByUserId(JOE_DOE_ID);
    assertEquals(3, actual.size());
  }*/

  private static List<app.bpartners.api.model.InvoiceProduct> creatableProds(int n) {
    List<app.bpartners.api.model.InvoiceProduct> result = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      result.add(prod());
    }
    return result;
  }

  private static app.bpartners.api.model.InvoiceProduct prod() {
    return app.bpartners.api.model.InvoiceProduct.builder()
        .id("product_id")
        .quantity(50)
        .description("product description")
        .vatPercent(new Fraction(BigInteger.ONE))
        .unitPrice(new Fraction(BigInteger.ONE))
        .priceNoVatWithDiscount(new Fraction())
        .totalWithDiscount(new Fraction())
        .build();
  }

  // TODO: use for local test only and set localstack for CI
  @Test
  void send_mail_ok() throws IOException {
    Resource attachmentResource = new ClassPathResource("files/modèle-facture.pdf");
    byte[] attachmentAsBytes = attachmentResource.getInputStream().readAllBytes();
    String attachmentName = "modèle-devis-v0.pdf";
    String otherAttachmentName = "modèle-devis_2-v0.pdf";
    Attachment attachment =
        Attachment.builder().name(attachmentName).content(attachmentAsBytes).build();
    Attachment secondAttachment =
        Attachment.builder().name(otherAttachmentName).content(attachmentAsBytes).build();
    String recipient = "bpartners.artisans@gmail.com";
    String subject = "Facture depuis l'API";
    String type = "facture";
    String htmlBody =
        "<html><body><h2 style=\"color:#660033;\">BPartners</h2> <h3"
            + " style=\"color:#e4dee0;\">l'assistant bancaire qui accélère la croissance et les"
            + " encaissements des artisans.</h3><p>Bonjour,</p><p>Retrouvez-ci joint votre "
            + type
            + ".</p>"
            + "<p>Bien à vous et merci pour votre confiance.</p>"
            + "</body></html>";
    /*
    TODO: DraftIT must only execute in local
    assertDoesNotThrow(() -> this.subject.verifyEmailIdentity(recipient));
    assertDoesNotThrow(
        () ->
            this.subject.sendEmail(
                recipient, null, subject, htmlBody, List.of(attachment, secondAttachment)));*/
  }

  @Test
  void generate_invoice_pdf_ok() {
    assertDoesNotThrow(() -> generatePdf("invoice"));
  }
}
