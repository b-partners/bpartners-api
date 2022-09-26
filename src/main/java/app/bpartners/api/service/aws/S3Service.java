package app.bpartners.api.service.aws;

import app.bpartners.api.service.AccountService;
import app.bpartners.api.service.utils.FileInfoUtils;
import java.io.File;
import java.io.IOException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

@Service
@AllArgsConstructor
@Slf4j
public class S3Service {
  private final S3Client s3Client;
  private final AccountService accountService;
  private static final String ENV = "dev"; //TODO: use the global environment variable
  private static final String LOGO_TYPE = "logo"; //TODO : put it in appropriate component

  private static final String BUCKET_NAME = "bpartners"; //TODO: put in properties

  public PutObjectResponse uploadFile(String fileId, byte[] toUpload) {
    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(BUCKET_NAME)
        .key(fileId)
        .contentType(FileInfoUtils.parseMediaType(fileId).getType())
        .build();
    return s3Client.putObject(request, RequestBody.fromBytes(toUpload));
  }

  public ResponseBytes<GetObjectResponse> downloadFile(String fileId) throws IOException {
    GetObjectRequest objectRequest = GetObjectRequest.builder()
        .bucket(BUCKET_NAME)
        .key(getLogoBucketName(fileId))
        .build();
    ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(objectRequest);
    return objectBytes;
  }

  public void deleteFile(String fileId) {
    DeleteObjectRequest objectRequest = DeleteObjectRequest.builder()
        .bucket(BUCKET_NAME)
        .key(getLogoBucketName(fileId))
        .build();
    s3Client.deleteObject(objectRequest);
  }

  private String getBucketName(String env, String fileId, String type) {
    String accountId = accountService.getAuthenticatedAccount().getId();
    return String.format("%s/accounts/%s/%s/%s", env, accountId, type, fileId);
  }

  private String getLogoBucketName(String fileId) {
    return getBucketName(ENV, fileId, LOGO_TYPE);
  }

  private File createTemporaryFile(String fileId) {
    return new File(String.format("src/main/resources/tmp/%s", fileId));
  }
}