package app.bpartners.api.service.aws;

import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.service.utils.FileInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Service
@Slf4j
public class S3Service {
  private static final String S3_KEY_FORMAT = "%s/accounts/%s/%s/%s";
  private final S3Client s3Client;
  private final S3Conf s3Conf;

  public S3Service(
      S3Client s3Client,
      S3Conf s3Conf) {
    this.s3Client = s3Client;
    this.s3Conf = s3Conf;
  }

  public String uploadFile(String accountId, String fileId, byte[] toUpload) {
    String computedFileId = getLogoBucketName(accountId, fileId);
    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(s3Conf.getBucketName())
        .contentType(FileInfoUtils.parseMediaTypeFromBytes(fileId, toUpload).toString())
        .checksumAlgorithm(ChecksumAlgorithm.SHA256)
        .key(computedFileId)
        .build();

    PutObjectResponse objectResponse = s3Client.putObject(request, RequestBody.fromBytes(toUpload));

    s3Client
        .waiter()
        .waitUntilObjectExists(
            HeadObjectRequest.builder()
                .bucket(s3Conf.getBucketName())
                .key(computedFileId)
                .build())
        .matched()
        .response()
        .ifPresent(response -> log.info("response={}", response));
    return objectResponse.checksumSHA256();
  }

  public byte[] downloadFile(String accountId, String fileId) {
    GetObjectRequest objectRequest = GetObjectRequest.builder()
        .bucket(s3Conf.getBucketName())
        .key(getLogoBucketName(accountId, fileId))
        .build();
    return s3Client.getObjectAsBytes(objectRequest).asByteArray();
  }

  private String getBucketName(String env, String accountId, String fileId, String type) {
    return String.format(S3_KEY_FORMAT, env, accountId, type, fileId);
  }

  private String getLogoBucketName(String accountId, String fileId) {
    return getBucketName(s3Conf.getEnv(), accountId, fileId,
        S3Conf.S3FileType.LOGO.name().toLowerCase());
  }

}