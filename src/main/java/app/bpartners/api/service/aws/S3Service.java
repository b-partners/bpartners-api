package app.bpartners.api.service.aws;

import static app.bpartners.api.endpoint.rest.model.FileType.AREA_PICTURE;
import static app.bpartners.api.endpoint.rest.model.FileType.ATTACHMENT;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.utils.FileInfoUtils;
import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.internal.waiters.ResponseOrException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

@Service
@Slf4j
@AllArgsConstructor
public class S3Service {
  // TODO: refactor using poja BucketComponent
  private static final String S3_KEY_FORMAT = "%s/accounts/%s/%s/%s";
  private final S3Conf s3Conf;
  private final UserRepository userRepository;

  public String getPresignedUrl(String key, Long expirationInSeconds) {
    Instant now = Instant.now();
    Instant expirationInstant = now.plusSeconds(expirationInSeconds);
    Duration expirationDuration = Duration.between(now, expirationInstant);

    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(s3Conf.getBucketName()).key(key).build();

    PresignedGetObjectRequest presignRequest =
        s3Conf
            .getS3Presigner()
            .presignGetObject(
                GetObjectPresignRequest.builder()
                    .signatureDuration(expirationDuration)
                    .getObjectRequest(getObjectRequest)
                    .build());

    return presignRequest.url().toString();
  }

  public String getPresignedUrl(
      FileType fileType, String idUser, String fileId, Long expirationInSeconds) {
    String key = getKey(idUser);
    return switch (fileType) {
      case TRANSACTION -> getPresignedUrl(getTransactionKey(fileId), expirationInSeconds);
      case LOGO -> getPresignedUrl(getLogoKey(key, fileId), expirationInSeconds);
      case INVOICE -> getPresignedUrl(getInvoiceKey(key, fileId), expirationInSeconds);
      case ATTACHMENT -> getPresignedUrl(getAttachmentKey(key, fileId), expirationInSeconds);
      default -> throw new BadRequestException("Unknown file type " + fileType);
    };
  }

  private String uploadFile(String key, byte[] toUpload) {
    log.info(
        "File to be upload into S3 for User(id="
            + AuthProvider.getAuthenticatedUserId()
            + ") with key {}",
        key);

    PutObjectRequest request =
        PutObjectRequest.builder()
            .bucket(s3Conf.getBucketName())
            .key(key)
            .contentType(FileInfoUtils.parseMediaTypeFromBytes(toUpload).toString())
            .checksumAlgorithm(s3Conf.getDefaultChecksumAlgorithm())
            .build();

    PutObjectResponse objectResponse =
        s3Conf.getS3Client().putObject(request, RequestBody.fromBytes(toUpload));

    ResponseOrException<HeadObjectResponse> responseOrException =
        s3Conf
            .getS3Client()
            .waiter()
            .waitUntilObjectExists(
                HeadObjectRequest.builder().bucket(s3Conf.getBucketName()).key(key).build())
            .matched();
    responseOrException
        .exception()
        .ifPresent(
            throwable -> {
              throw new ApiException(SERVER_EXCEPTION, throwable.getMessage());
            });
    responseOrException.response().ifPresent(response -> log.info("response={}", response));

    return objectResponse.checksumSHA256();
  }

  public String uploadFile(FileType fileType, String idUser, String fileId, byte[] toUpload) {
    String key = getKey(idUser);
    return switch (fileType) {
      case TRANSACTION, TRANSACTION_SUPPORTING_DOCS -> uploadFile(
          getTransactionKey(fileId), toUpload);
      case LOGO -> uploadFile(getLogoKey(key, fileId), toUpload);
      case INVOICE -> uploadFile(getInvoiceKey(key, fileId), toUpload);
      case ATTACHMENT -> uploadFile(getAttachmentKey(key, fileId), toUpload);
      case AREA_PICTURE -> uploadFile(getAreaPictureKey(key, fileId), toUpload);
      default -> throw new BadRequestException("Unknown file type " + fileType);
    };
  }

  private byte[] downloadFile(String key) {
    GetObjectRequest objectRequest;
    try {
      objectRequest = GetObjectRequest.builder().bucket(s3Conf.getBucketName()).key(key).build();
      return s3Conf.getS3Client().getObjectAsBytes(objectRequest).asByteArray();
    } catch (NoSuchKeyException e) {
      log.warn("S3 File not found, key to find was : {}", key);
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  public byte[] downloadFile(FileType fileType, String idUser, String fileId) {
    String key = getKey(idUser);
    return switch (fileType) {
      case TRANSACTION, TRANSACTION_SUPPORTING_DOCS -> downloadFile(getTransactionKey(fileId));
      case LOGO -> downloadFile(getLogoKey(key, fileId));
      case INVOICE -> downloadFile(getInvoiceKey(key, fileId));
      case ATTACHMENT -> downloadFile(getAttachmentKey(key, fileId));
      case AREA_PICTURE -> downloadFile(getAreaPictureKey(key, fileId));
      default -> throw new BadRequestException("Unknown file type " + fileType);
    };
  }

  private String getKey(String idUser) {
    return userRepository.getById(idUser).getOldS3key();
  }

  private String getBucketName(String env, String idUser, String fileId, String type) {
    return String.format(S3_KEY_FORMAT, env, idUser, type, fileId);
  }

  private String getTransactionKey(String fileId) {
    return "transactions/" + fileId;
  }

  private String getLogoKey(String idUser, String fileId) {
    return getBucketName(s3Conf.getEnv(), idUser, fileId, LOGO.name().toLowerCase());
  }

  private String getInvoiceKey(String idUser, String fileId) {
    return getBucketName(s3Conf.getEnv(), idUser, fileId, INVOICE.name().toLowerCase());
  }

  private String getAttachmentKey(String idUser, String fileId) {
    return getBucketName(s3Conf.getEnv(), idUser, fileId, ATTACHMENT.name().toLowerCase());
  }

  private String getAreaPictureKey(String idUser, String fileId) {
    return getBucketName(s3Conf.getEnv(), idUser, fileId, AREA_PICTURE.name().toLowerCase());
  }
}
