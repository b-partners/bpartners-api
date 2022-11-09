package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.FileSaved;
import app.bpartners.api.endpoint.rest.model.FileType;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class FileSavedService implements Consumer<FileSaved> {
  private final FileService fileService;

  @Override
  public void accept(FileSaved fileSaved) {
    FileType fileType = fileSaved.getFileType();
    String accountId = fileSaved.getAccountId();
    String fileId = fileSaved.getFileId();
    byte[] fileAsBytes = fileSaved.getFileAsBytes();
    fileService.upload(fileId, fileType, accountId, fileAsBytes);
  }
}
