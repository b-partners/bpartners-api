package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.FileSaved;
import app.bpartners.api.endpoint.rest.model.FileType;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
@Slf4j
public class FileSavedService implements Consumer<FileSaved> {
  private final FileService fileService;

  @Transactional(isolation = Isolation.SERIALIZABLE)
  @Override
  public void accept(FileSaved fileSaved) {
    FileType fileType = fileSaved.getFileType();
    String accountId = fileSaved.getAccountId();
    String fileId = fileSaved.getFileId();
    String userId = fileSaved.getUserId();
    byte[] fileAsBytes = fileSaved.getFileAsBytes();
    fileService.upload(fileId, fileType, accountId, fileAsBytes, userId);
  }
}
