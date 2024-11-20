package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static java.time.LocalDate.now;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.model.InvoiceExportLinkRequested;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.InvoiceExportLinkRequestedService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import javax.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class InvoiceExportLinkRequestedServiceIT extends MockedThirdParties {
  @Autowired InvoiceExportLinkRequestedService subject;
  @MockBean S3Service s3ServiceMock;
  @MockBean SesService mailerMock;

  @BeforeEach
  void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
  }

  @Test
  void accept_ok() throws MessagingException, IOException {
    var tempFile =
        crupdateFile(Paths.get("src", "test", "resources", "files", "REFinvoiceId1.pdf").toFile());
    when(s3ServiceMock.downloadFile(any(), any(), any())).thenReturn(tempFile);

    subject.accept(
        InvoiceExportLinkRequested.builder()
            .userId(JOE_DOE_ID)
            .providedFrom(now().minusYears(3L))
            .providedTo(now())
            .page(0)
            .build());

    verify(s3ServiceMock, times(1)).uploadFile(any(), any(), any(), any());
    verify(mailerMock, times(1)).sendEmail(any(), any(), any(), any());
  }

  private File crupdateFile(File file) {
    if (!file.exists()) {
      try {
        boolean fileCreated = file.createNewFile();
        if (!fileCreated) {
          return file;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return file;
  }
}
