package app.bpartners.api.service.aws;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.file.BucketComponent;
import app.bpartners.api.file.BucketKeyRetriever;
import app.bpartners.api.file.FileHash;
import java.io.File;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class S3Service {
  private final BucketComponent bucketComponent;
  private final BucketKeyRetriever bucketKeyRetriever;

  public String presignURL(
      FileType fileType, String fileId, String idUser, Long expirationInSeconds) {
    String key = bucketKeyRetriever.apply(fileType, fileId, idUser);
    return bucketComponent.presign(key, Duration.ofSeconds(expirationInSeconds)).toString();
  }

  @SneakyThrows
  public FileHash uploadFile(FileType fileType, String fileId, String idUser, File fileToUpload) {
    String key = bucketKeyRetriever.apply(fileType, fileId, idUser);
    return bucketComponent.upload(fileToUpload, key);
  }

  public File downloadFile(FileType fileType, String fileId, String idUser) {
    String key = bucketKeyRetriever.apply(fileType, fileId, idUser);
    return bucketComponent.download(key);
  }
}
