package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.LogCaptor;
import app.bpartners.api.endpoint.event.model.LogoCompressionTriggered;
import app.bpartners.api.service.file.FileService;
import app.bpartners.api.service.file.LogoService;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import java.io.File;
import java.util.List;
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

  @Test
  void log_warn_when_logoFileId_is_null() {
    LogoCompressionTriggered event = new LogoCompressionTriggered(USER_ID, null);
    LogCaptor logCaptor = new LogCaptor();
    logCaptor.configure(LogoCompressionTriggeredService.class);

    subject.accept(event);

    List<ILoggingEvent> logEvents = logCaptor.getLogEvents();
    assertTrue(
        logEvents.stream()
            .anyMatch(
                log ->
                    log.getLevel() == Level.WARN
                        && log.getFormattedMessage().contains("User." + USER_ID + " has no logo")));
    verifyNoInteractions(fileService);
    verify(logoService, never()).compressUserLogo(any(), any(), any());
  }

  @Test
  void log_info_when_logo_is_already_compressed() {
    LogoCompressionTriggered event = new LogoCompressionTriggered(USER_ID, FILE_ID);
    when(logoService.isCompressedLogo(FILE_ID)).thenReturn(true);
    LogCaptor logCaptor = new LogCaptor();
    logCaptor.configure(LogoCompressionTriggeredService.class);

    subject.accept(event);

    List<ILoggingEvent> logEvents = logCaptor.getLogEvents();
    assertTrue(
        logEvents.stream()
            .anyMatch(
                log ->
                    log.getLevel() == Level.INFO
                        && log.getFormattedMessage()
                            .contains("User." + USER_ID + " already has a compressed logo")));
    verifyNoInteractions(fileService);
    verify(logoService, never()).compressUserLogo(any(), any(), any());
  }
}
