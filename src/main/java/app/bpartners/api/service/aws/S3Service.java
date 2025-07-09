package app.bpartners.api.service.aws;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.file.BucketKeyRetriever;
import app.bpartners.api.file.bucket.BucketComponent;
import app.bpartners.api.file.hash.FileHash;
import java.io.File;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
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
    return bucketComponent.upload(fileToUpload, key, true);
  }

  public File downloadFile(FileType fileType, String fileId, String idUser) {
    String key = bucketKeyRetriever.apply(fileType, fileId, idUser);
    return bucketComponent.download(key, true);
  }

  public File downloadLandingFile(String key) {
    return bucketComponent.download(key, false);
  }

  public String uploadLandingFile(File file, String key) {
    bucketComponent.upload(file, key, false);
    return bucketComponent.presignLanding(key, Duration.ofSeconds(60)).toString();
  }
}
