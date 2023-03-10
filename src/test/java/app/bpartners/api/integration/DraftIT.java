package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.InvoicePdfUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum.IN_INSTALMENT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = DraftIT.ContextInitializer.class)
@AutoConfigureMockMvc
class DraftIT {
  @MockBean
  private BuildingPermitConf buildingPermitConf;
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private SwanConf swanConf;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private UserSwanRepository userSwanRepositoryMock;
  @MockBean
  private AccountSwanRepository accountSwanRepositoryMock;
  @MockBean
  private SwanComponent swanComponentMock;
  @Autowired
  private SesService subject;
  @Autowired
  private LegalFileRepository legalFileRepository;

  private static void generatePdf(String templateName) throws IOException {
    app.bpartners.api.model.Invoice invoice = Invoice.builder()
        .id(INVOICE1_ID)
        .ref("invoice_ref")
        .title("invoice_title")
        .status(PROPOSAL)
        .sendingDate(LocalDate.now())
        .toPayAt(LocalDate.now())
        .account(Account.builder()
            .id(JOE_DOE_ACCOUNT_ID)
            .name("BPartners")
            .iban("FR7630001007941234567890185")
            .bic("BPFRPP751")
            .build())
        .paymentType(IN_INSTALMENT)
        .multiplePayments(List.of(
            CreatePaymentRegulation.builder()
                .comment(null)
                .maturityDate(LocalDate.now().plusDays(1L))
                .amount(new Fraction(BigInteger.valueOf(100)))
                .paymentUrl("https://connect-v2-sbx.fintecture.com")
                .build(),
            CreatePaymentRegulation.builder()
                .comment("Avec un assez long commentaire pour voir si ça descend automatiquement")
                .maturityDate(LocalDate.now())
                .amount(new Fraction(BigInteger.TEN))
                .paymentUrl("https://connect-v2-sbx.fintecture.com")
                .build()))
        .products(creatableProds(3))
        .customer(Customer.builder()
            .firstName("Olivier")
            .lastName("Durant")
            .phone("+33 6 12 45 89 76")
            .email("exemple@email.com")
            .address("Paris 745")
            .build())
        .totalPriceWithVat(new Fraction(BigInteger.ONE))
        .totalPriceWithoutVat(new Fraction(BigInteger.ONE))
        .totalVat(new Fraction(BigInteger.ONE))
        .paymentUrl("text")
        .build();
    InvoicePdfUtils pdfUtils = new InvoicePdfUtils();
    byte[] logoAsBytes =
        new ClassPathResource("files/downloaded.jpeg").getInputStream().readAllBytes();
    AccountHolder accountHolder = AccountHolder.builder()
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
        .totalPriceWithVat(new Fraction(BigInteger.ONE))
        .build();
  }

  //TODO: use for local test only and set localstack for CI
  @Test
  void send_mail_ok() throws IOException {
    Resource attachmentResource = new ClassPathResource("files/modèle-facture.pdf");
    byte[] attachmentAsBytes = attachmentResource.getInputStream().readAllBytes();
    String attachmentName = "modèle-devis-v0.pdf";
    String otherAttachmentName = "modèle-devis_2-v0.pdf";
    Attachment attachment = Attachment.builder()
        .name(attachmentName)
        .content(attachmentAsBytes)
        .build();
    Attachment secondAttachment = Attachment.builder()
        .name(otherAttachmentName)
        .content(attachmentAsBytes)
        .build();
    String recipient = "bpartners.artisans@gmail.com";
    String subject = "Facture depuis l'API";
    String type = "facture";
    String htmlBody = "<html>"
        + "<body>"
        + "<h2 style=\"color:#660033;\">BPartners</h2> <h3 style=\"color:#e4dee0;\">l'assistant " +
        "bancaire qui accélère la croissance et les encaissements des artisans.</h3>"
        + "<p>Bonjour,</p>"
        + "<p>Retrouvez-ci joint votre " + type + ".</p>"
        + "<p>Bien à vous et merci pour votre confiance.</p>"
        + "</body></html>";
    assertDoesNotThrow(() -> this.subject.verifyEmailIdentity(recipient));
    assertDoesNotThrow(() -> this.subject.sendEmail(recipient, subject, htmlBody,
        List.of(attachment, secondAttachment)));
  }

  @Test
  void generate_invoice_pdf_ok() {
    assertDoesNotThrow(() -> generatePdf("invoice"));
  }

  @Test
  void generate_draft_pdf_ok() {
    assertDoesNotThrow(() -> generatePdf("draft"));
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
