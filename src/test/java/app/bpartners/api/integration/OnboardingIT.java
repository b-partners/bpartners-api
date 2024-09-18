package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bpartners.api.endpoint.rest.api.OnboardingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.VisitorEmail;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.service.aws.SesService;
import java.io.IOException;
import java.util.List;
import javax.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@Slf4j
class OnboardingIT extends MockedThirdParties {
  @MockBean SesService mailerMock;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  VisitorEmail toSend() {
    return new VisitorEmail()
        .email("test@gmail.com")
        .subject("Test subject")
        .firstName("John")
        .lastName("Wicks")
        .comments("Stay on feet until last breathe");
  }

  @Test
  void visitor_send_email_ok() throws ApiException, MessagingException, IOException {
    ApiClient client = anApiClient();
    OnboardingApi api = new OnboardingApi(client);

    var actual = api.visitorSendEmail(toSend());
    verify(mailerMock, times(1))
        .sendEmail(
            "dummy",
            toSend().getEmail(),
            toSend().getSubject(),
            String.format(
                "<div><p>%s</p></div></br></br><h2>%s %s</h2>",
                toSend().getComments(), toSend().getFirstName(), toSend().getLastName()),
            List.of());

    assertEquals(toSend(), actual);
  }

  @Test
  void visitor_send_email_ko() {
    ApiClient client = anApiClient();
    OnboardingApi api = new OnboardingApi(client);
    var invalidEmail = toSend().email(null).comments(null).subject(null);

    assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Email is mandatory. Subject is mandatory."
            + " Comments are mandatory.\"}",
        () -> api.visitorSendEmail(invalidEmail));
  }
}
