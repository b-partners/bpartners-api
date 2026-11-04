package app.bpartners.api.file.bucket;

import java.net.URL;
import java.time.Duration;
import java.util.OptionalLong;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
@AllArgsConstructor
public class BucketUploadComponent {
  private final CustomBucketConf customBucketConf;

  public URL presignUpload(String bucketKey, long contentLength, Duration expiration) {
    var putObjectRequest =
        PutObjectRequest.builder()
            .bucket(customBucketConf.getBucketName())
            .key(bucketKey)
            .contentLength(contentLength)
            .build();
    return customBucketConf
        .getS3Presigner()
        .presignPutObject(
            PutObjectPresignRequest.builder()
                .signatureDuration(expiration)
                .putObjectRequest(putObjectRequest)
                .build())
        .url();
  }

  public OptionalLong contentLength(String bucketKey) {
    try {
      var response =
          customBucketConf
              .getS3Client()
              .headObject(
                  HeadObjectRequest.builder()
                      .bucket(customBucketConf.getBucketName())
                      .key(bucketKey)
                      .build());
      return OptionalLong.of(response.contentLength());
    } catch (NoSuchKeyException e) {
      return OptionalLong.empty();
    }
  }
}
