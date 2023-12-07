package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.MailingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateEmail;
import app.bpartners.api.endpoint.rest.model.Email;
import app.bpartners.api.endpoint.rest.model.EmailStatus;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = DbEnvContextInitializer.class)
@Slf4j
class MailingIT extends MockedThirdParties {
  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN,
        DbEnvContextInitializer.getHttpServerPort());
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  @Test
  void read_and_edit_emails_ok() throws ApiException {
    MailingApi api = new MailingApi(anApiClient());

    List<Email> actual1 = api.readEmails(JOE_DOE_ID, EmailStatus.DRAFT, MIN_PAGE, MAX_SIZE);
    List<Email> actual2 = api.editOrSendEmails(JOE_DOE_ID, List.of(new CreateEmail()
        .id("email1_id")
        .emailObject("Object 1")
        .recipients(List.of("tech@bpartners.app"))
        .emailBody("<p>Hello</p>")
        .status(EmailStatus.DRAFT)
        .attachments(List.of())
    ));
    List<Email> actual3 = api.readEmails(JOE_DOE_ID, EmailStatus.DRAFT, MIN_PAGE, MAX_SIZE);

    assertTrue(actual1.isEmpty());
    assertEquals(1, actual2.size());
    assertTrue(actual3.containsAll(actual2));
  }
}
