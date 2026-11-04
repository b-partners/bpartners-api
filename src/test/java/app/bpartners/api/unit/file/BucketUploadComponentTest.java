package app.bpartners.api.unit.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.file.bucket.BucketUploadComponent;
import app.bpartners.api.file.bucket.CustomBucketConf;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

class BucketUploadComponentTest {
  private static final String BUCKET = "test-bucket";
  private final CustomBucketConf customBucketConfMock = mock(CustomBucketConf.class);
  private final S3Client s3ClientMock = mock(S3Client.class);
  private final BucketUploadComponent subject = new BucketUploadComponent(customBucketConfMock);

  BucketUploadComponentTest() {
    var presigner =
        S3Presigner.builder()
            .region(Region.EU_WEST_3)
            .credentialsProvider(
                StaticCredentialsProvider.create(AwsBasicCredentials.create("key", "secret")))
            .build();
    when(customBucketConfMock.getBucketName()).thenReturn(BUCKET);
    when(customBucketConfMock.getS3Presigner()).thenReturn(presigner);
    when(customBucketConfMock.getS3Client()).thenReturn(s3ClientMock);
  }

  @Test
  void presigned_upload_url_targets_key_and_signs_content_length() {
    var url = subject.presignUpload("prospects/p/file", 12_345L, Duration.ofMinutes(15));

    assertTrue(url.getHost().startsWith(BUCKET));
    assertEquals("/prospects/p/file", url.getPath());
    var query = url.getQuery();
    assertTrue(query.contains("X-Amz-Expires=900"), query);
    assertTrue(query.toLowerCase().contains("content-length"), query);
  }

  @Test
  void content_length_is_read_from_head_object() {
    when(s3ClientMock.headObject(HeadObjectRequest.builder().bucket(BUCKET).key("k").build()))
        .thenReturn(HeadObjectResponse.builder().contentLength(42L).build());

    assertEquals(42L, subject.contentLength("k").getAsLong());
  }

  @Test
  void content_length_is_empty_when_object_is_missing() {
    when(s3ClientMock.headObject(HeadObjectRequest.builder().bucket(BUCKET).key("k").build()))
        .thenThrow(NoSuchKeyException.builder().build());

    assertTrue(subject.contentLength("k").isEmpty());
  }
}
