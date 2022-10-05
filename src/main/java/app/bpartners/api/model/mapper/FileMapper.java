package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.repository.jpa.model.HFileInfo;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FileMapper {

  private final PrincipalProvider provider;

  public app.bpartners.api.endpoint.rest.model.FileInfo toRest(FileInfo internal) {
    return new app.bpartners.api.endpoint.rest.model.FileInfo()
        .id(internal.getId())
        .uploadedAt(internal.getUploadedAt())
        .uploadedByAccountId(internal.getUploadedBy())
        .sizeInKB(internal.getSizeInKB())
        .sha256(internal.getSha256());
  }

  public FileInfo toDomain(String fileId, byte[] toUpload, String checksum) {
    return FileInfo.builder()
        .id(fileId)
        .uploadedAt(Instant.now())
        .uploadedBy(((Principal) provider.getAuthentication().getPrincipal()).getAccount().getId())
        .sizeInKB(toUpload.length / 1024)
        .sha256(checksum)
        .build();
  }

  public FileInfo toDomain(HFileInfo file) {
    return FileInfo.builder()
        .id(file.getId())
        .uploadedAt(file.getUploadedAt())
        .uploadedBy(file.getAccountId())
        .sizeInKB(file.getSizeInKB())
        .sha256(file.getSha256())
        .build();
  }

  public HFileInfo toEntity(FileInfo file) {
    return HFileInfo.builder()
        .id(file.getId())
        .uploadedAt(file.getUploadedAt())
        .sizeInKB(file.getSizeInKB())
        .sha256(file.getSha256())
        .accountId(file.getUploadedBy())
        .build();
  }


}
