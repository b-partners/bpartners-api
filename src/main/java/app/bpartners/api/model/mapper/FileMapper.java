package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HFileInfo;
import app.bpartners.api.repository.jpa.model.HUser;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Component
@AllArgsConstructor
public class FileMapper {

  private final PrincipalProvider provider;

  private final UserJpaRepository userJpaRepository;

  public app.bpartners.api.endpoint.rest.model.FileInfo toRest(FileInfo internal) {
    return new app.bpartners.api.endpoint.rest.model.FileInfo()
        .id(internal.getId())
        .uploadedAt(internal.getUploadedAt())
        .uploadedByUserId(internal.getUploadedBy())
        .sizeInKB(internal.getSizeInKB())
        .sha256(internal.getSha256());
  }

  public FileInfo toDomain(String fileId, byte[] toUpload, PutObjectResponse objectResponse) {
    return FileInfo.builder()
        .id(fileId)
        .uploadedAt(Instant.now())
        .uploadedBy(((Principal) provider.getAuthentication().getPrincipal()).getUserId())
        .sizeInKB(toUpload.length / 1024)
        .sha256("some checksum")//TODO: checksum SHA256
        .build();
  }

  public FileInfo toDomain(HFileInfo file) {
    return FileInfo.builder()
        .id(file.getId())
        .uploadedAt(file.getUploadedAt())
        .uploadedBy(file.getUploadedBy().getId())
        .sizeInKB(file.getSizeInKB())
        .sha256(file.getSha256())
        .build();
  }

  public HFileInfo toEntity(FileInfo file) {
    HUser user = userJpaRepository.getById(file.getUploadedBy());
    return HFileInfo.builder()
        .id(file.getId())
        .uploadedAt(file.getUploadedAt())
        .uploadedBy(user)
        .sizeInKB(file.getSizeInKB())
        .sha256(file.getSha256())
        .build();
  }

  private String hash256(byte[] toHash) throws NoSuchAlgorithmException {
    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
    byte[] hashedByte = messageDigest.digest(toHash);
    StringBuilder result = new StringBuilder();
    for (byte b : hashedByte) {
      result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
    }
    return result.toString();
  }
}
