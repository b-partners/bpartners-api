package app.bpartners.api.service.aws;

import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.utils.FileInfoUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ChecksumAlgorithm;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static app.bpartners.api.endpoint.rest.model.FileType.ATTACHMENT;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;


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

  private String uploadFile(String key, byte[] toUpload) {
    PutObjectRequest request = PutObjectRequest.builder()
        .bucket(s3Conf.getBucketName())
        .contentType(FileInfoUtils.parseMediaTypeFromBytes(toUpload).toString())
        .checksumAlgorithm(ChecksumAlgorithm.SHA256)
        .key(key)
        .build();

    PutObjectResponse objectResponse = s3Client.putObject(request, RequestBody.fromBytes(toUpload));

    ResponseOrException<HeadObjectResponse> responseOrException = s3Client
        .waiter()
        .waitUntilObjectExists(
            HeadObjectRequest.builder()
                .bucket(s3Conf.getBucketName())
                .key(key)
                .build())
        .matched();
    responseOrException.exception().ifPresent(throwable -> {
      throw new ApiException(SERVER_EXCEPTION, throwable.getMessage());
    });
    responseOrException.response().ifPresent(response ->
        log.info("response={}", response));

    return objectResponse.checksumSHA256();
  }

  public String uploadFile(FileType fileType, String idUser, String fileId, byte[] toUpload) {
    String key = AuthProvider.getAuthenticatedUser().getOldS3key();
    switch (fileType) {
      case LOGO:
        return uploadFile(getLogoKey(key, fileId), toUpload);
      case INVOICE:
        return uploadFile(getInvoiceKey(key, fileId), toUpload);
      case ATTACHMENT:
        return uploadFile(getAttachmentKey(key, fileId), toUpload);
      default:
        throw new BadRequestException("Unknown file type " + fileType);
    }
  }

  private byte[] downloadFile(String key) {
    GetObjectRequest objectRequest;
    try {
      objectRequest = GetObjectRequest.builder()
          .bucket(s3Conf.getBucketName())
          .key(key)
          .build();
      return s3Client.getObjectAsBytes(objectRequest).asByteArray();
    } catch (NoSuchKeyException e) {
      log.warn("S3 File not found, key to find was : {}", key);
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public byte[] downloadFile(FileType fileType, String idUser, String fileId) {
    String key = AuthProvider.getAuthenticatedUser().getOldS3key();
    switch (fileType) {
      case LOGO:
        return downloadFile(getLogoKey(key, fileId));
      case INVOICE:
        return downloadFile(getInvoiceKey(key, fileId));
      case ATTACHMENT:
        return downloadFile(getAttachmentKey(key, fileId));
      default:
        throw new BadRequestException("Unknown file type " + fileType);
    }
  }

  private String getBucketName(String env, String idUser, String fileId, String type) {
    return String.format(S3_KEY_FORMAT, env, idUser, type, fileId);
  }

  private String getLogoKey(String idUser, String fileId) {
    return getBucketName(s3Conf.getEnv(), idUser, fileId,
        LOGO.name().toLowerCase());
  }

  private String getInvoiceKey(String idUser, String fileId) {
    return getBucketName(s3Conf.getEnv(), idUser, fileId,
        INVOICE.name().toLowerCase());
  }

  private String getAttachmentKey(String idUser, String fileId) {
    return getBucketName(s3Conf.getEnv(), idUser, fileId,
        ATTACHMENT.name().toLowerCase());
  }
}