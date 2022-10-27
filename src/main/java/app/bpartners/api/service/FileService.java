package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedFileUploaded;
import app.bpartners.api.endpoint.event.model.gen.FileUploaded;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.service.aws.S3Service;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class FileService {
  private final S3Service s3Service;
  private final FileRepository repository;
  private final FileMapper mapper;
  private final EventProducer eventProducer;

  public void uploadFile(FileType fileType, String accountId, String fileId, byte[] toUpload) {
    eventProducer.accept(List.of(
        toTypedEvent(fileType, accountId, fileId, toUpload, null)));
    repository.save(mapper.toDomain(fileId, toUpload, null));
  }

  public byte[] downloadFile(FileType fileType, String accountId, String fileId) {
    getFileByAccountIdAndId(accountId, fileId);
    return s3Service.downloadFile(fileType, accountId, fileId);
  }

  public FileInfo getFileByAccountIdAndId(String accountId, String fileId) {
    return repository.getByAccountIdAndId(accountId, fileId);
  }

  public FileInfo saveChecksum(
      String fileId, FileType fileType, String accountId, byte[] fileAsBytes) {
    String checksum = s3Service.uploadFile(fileType, accountId, fileId, fileAsBytes);
    FileInfo persisted = repository.getById(fileId);
    return repository.save(FileInfo.builder()
        .id(persisted.getId())
        .sizeInKB(persisted.getSizeInKB())
        .uploadedAt(persisted.getUploadedAt())
        .uploadedBy(persisted.getUploadedBy())
        .sha256(checksum)
        .build());
  }

  public TypedFileUploaded toTypedEvent(
      FileType fileType, String accountId, String fileId, byte[] toUpload,
      String invoiceId) {
    return new TypedFileUploaded(
        FileUploaded.builder()
            .fileId(fileId)
            .accountId(accountId)
            .fileType(fileType)
            .fileAsBytes(toUpload)
            .invoiceId(invoiceId)
            .build()
    );
  }
}
