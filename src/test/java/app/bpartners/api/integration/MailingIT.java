package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.api.MailingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreateEmail;
import app.bpartners.api.endpoint.rest.model.Email;
import app.bpartners.api.endpoint.rest.model.EmailStatus;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Slf4j
@Disabled("TODO(fail)")
class MailingIT extends MockedThirdParties {
  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
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
    List<Email> actual2 =
        api.editOrSendEmails(
            JOE_DOE_ID,
            List.of(
                new CreateEmail()
                    .id("email1_id")
                    .emailObject("Object 1")
                    .recipients(List.of("tech@bpartners.app"))
                    .emailBody("<p>Hello</p>")
                    .status(EmailStatus.DRAFT)
                    .attachments(List.of())));
    List<Email> actual3 = api.readEmails(JOE_DOE_ID, EmailStatus.DRAFT, MIN_PAGE, MAX_SIZE);

    assertTrue(actual1.isEmpty());
    assertEquals(1, actual2.size());
    assertTrue(actual3.containsAll(actual2));
  }
}
