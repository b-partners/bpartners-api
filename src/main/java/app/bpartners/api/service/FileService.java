package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.TypedFileSaved;
import app.bpartners.api.endpoint.event.model.gen.FileSaved;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.mapper.FileMapper;
import app.bpartners.api.repository.FileRepository;
import app.bpartners.api.service.aws.S3Service;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class FileService {
  private final S3Service s3Service;
  private final FileRepository repository;
  private final FileMapper mapper;
  private final EventProducer eventProducer;

  public void uploadEvent(FileType fileType, String accountId, String fileId, byte[] toUpload) {
    repository.save(mapper.toDomain(fileId, toUpload, null, accountId));
    eventProducer.accept(List.of(
        toTypedEvent(fileType, accountId, fileId, toUpload)));
  }

  public FileInfo upload(String fileId, FileType fileType, String accountId, byte[] fileAsBytes) {
    String sha256 = s3Service.uploadFile(fileType, accountId, fileId, fileAsBytes);
    //TODO: add test for this
    Optional<FileInfo> optional = repository.getOptionalById(fileId);
    if (optional.isPresent()) {
      FileInfo persisted = optional.get();
      return repository.save(FileInfo.builder()
          .id(persisted.getId())
          .sizeInKB(persisted.getSizeInKB())
          .uploadedAt(persisted.getUploadedAt())
          .uploadedBy(persisted.getUploadedBy())
          .sha256(sha256)
          .build());
    }
    return repository.save(mapper.toDomain(fileId, fileAsBytes, sha256, accountId));
  }

  public byte[] downloadFile(FileType fileType, String accountId, String fileId) {
    getFileByAccountIdAndId(accountId, fileId);
    return s3Service.downloadFile(fileType, accountId, fileId);
  }

  public FileInfo getFileByAccountIdAndId(String accountId, String fileId) {
    return repository.getByAccountIdAndId(accountId, fileId);
  }

  public TypedFileSaved toTypedEvent(
      FileType fileType, String accountId, String fileId, byte[] toUpload) {
    return new TypedFileSaved(
        FileSaved.builder()
            .fileId(fileId)
            .accountId(accountId)
            .fileType(fileType)
            .fileAsBytes(toUpload)
            .build()
    );
  }
}
