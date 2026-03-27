package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.model.LogoCompressionTriggered;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.file.LogoService;
import java.io.File;
import org.junit.jupiter.api.Test;

class LogoCompressionTriggeredServiceTest {
  static final String USER_ID = randomUUID().toString();
  static final String FILE_ID = randomUUID().toString();

  LogoService logoService = mock();
  FileService fileService = mock();

  LogoCompressionTriggeredService subject =
      new LogoCompressionTriggeredService(logoService, fileService);

  @Test
  void call_related_services() {
    File downloadedFile = mock();
    LogoCompressionTriggered typedEvent = new LogoCompressionTriggered(USER_ID, FILE_ID);
    when(fileService.downloadFile(any(), any(), any())).thenReturn(downloadedFile);
    when(logoService.compressUserLogo(any(), any(), any())).thenReturn(mock());

    subject.accept(typedEvent);
    verify(logoService).compressUserLogo(USER_ID, downloadedFile, FILE_ID);
  }
}
