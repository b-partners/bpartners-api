package app.bpartners.api.unit.service;

import app.bpartners.api.endpoint.event.model.gen.FileSaved;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.service.FileSavedService;
import app.bpartners.api.service.FileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class FileSavedServiceTest {
  FileSavedService fileSavedService;
  FileService fileService;

  @BeforeEach
  void setUp() {
    fileService = mock(FileService.class);
    fileSavedService = new FileSavedService(fileService);
  }

  @Test
  void persistFileId_trigger_ok() {
    String fileId = "fileId.jpg";
    FileType fileType = INVOICE;
    String accountId = JOE_DOE_ACCOUNT_ID;
    byte[] fileAsBytes = new byte[0];
    doNothing().when(fileService).uploadEvent(any(), any(), any(), any());

    fileSavedService.accept(FileSaved.builder()
        .fileId(fileId)
        .fileType(fileType)
        .accountId(accountId)
        .fileAsBytes(fileAsBytes)
        .build());

    verify(fileService, times(1)).upload(fileId, fileType, accountId, fileAsBytes);
  }
}
