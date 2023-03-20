package app.bpartners.api.model.mapper;

import app.bpartners.api.model.FileInfo;
import app.bpartners.api.repository.jpa.model.HFileInfo;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class FileMapper {

  public app.bpartners.api.endpoint.rest.model.FileInfo toRest(FileInfo internal) {
    if (internal == null) {
      return null;
    }
    return new app.bpartners.api.endpoint.rest.model.FileInfo()
        .id(internal.getId())
        .uploadedAt(internal.getUploadedAt())
        .uploadedByAccountId(internal.getUploadedBy())
        .sizeInKB(internal.getSizeInKb())
        .sha256(internal.getSha256());
  }

  public FileInfo toDomain(String fileId, byte[] toUpload, String sha256, String accountId) {
    return FileInfo.builder()
        .id(fileId)
        .uploadedAt(Instant.now())
        .uploadedBy(accountId)
        .sizeInKb(toUpload.length / 1024)
        .sha256(sha256)
        .build();
  }

  public FileInfo toDomain(HFileInfo file) {
    return FileInfo.builder()
        .id(file.getId())
        .uploadedAt(file.getUploadedAt())
        .uploadedBy(file.getAccountId())
        .sizeInKb(file.getSizeInKB())
        .sha256(file.getSha256())
        .build();
  }

  public HFileInfo toEntity(FileInfo file) {
    return HFileInfo.builder()
        .id(file.getId())
        .uploadedAt(file.getUploadedAt())
        .sizeInKB(file.getSizeInKb())
        .sha256(file.getSha256())
        .accountId(file.getUploadedBy())
        .build();
  }


}
