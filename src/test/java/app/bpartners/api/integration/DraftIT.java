package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.service.aws.SesService;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = DraftIT.ContextInitializer.class)
@AutoConfigureMockMvc
class DraftIT {
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

  //TODO: use for local test only and set localstack for CI
  @Test
  void send_mail_ok() throws IOException {
    Resource attachmentResource = new ClassPathResource("files/modèle-facture.pdf");
    byte[] attachmentAsBytes = attachmentResource.getInputStream().readAllBytes();
    String attachmentName = "modèle-devis-v0.pdf";
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
        attachmentName, attachmentAsBytes));
  }

  public static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
