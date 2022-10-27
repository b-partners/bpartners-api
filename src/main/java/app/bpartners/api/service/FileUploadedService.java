package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.FileUploaded;
import app.bpartners.api.endpoint.rest.model.FileType;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class FileUploadedService implements Consumer<FileUploaded> {
  private final FileService fileService;
  private final InvoiceService invoiceService;

  @Override
  public void accept(FileUploaded fileUploaded) {
    String invoiceId = fileUploaded.getInvoiceId();
    FileType fileType = fileUploaded.getFileType();
    String accountId = fileUploaded.getAccountId();
    String fileId = fileUploaded.getFileId();
    byte[] fileAsBytes = fileUploaded.getFileAsBytes();
    if (invoiceId != null) {
      invoiceService.persistFileId(invoiceId);
    }
    fileService.saveChecksum(fileId, fileType, accountId, fileAsBytes);
  }
}
