package app.bpartners.api.file;

import static app.bpartners.api.endpoint.rest.model.FileType.AREA_PICTURE;
import static app.bpartners.api.endpoint.rest.model.FileType.ATTACHMENT;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE_ZIP;
import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class BucketKeyRetriever {
  private static final String TRANSACTION_DIR_FORMAT = "transactions/";
  private final UserRepository userRepository;
  private final String env;

  public BucketKeyRetriever(UserRepository userRepository, @Value("${env}") String env) {
    this.userRepository = userRepository;
    this.env = env;
  }

  public String apply(FileType fileType, String fileId, String userId) {
    var key = getUserS3Path(userId);
    return switch (fileType) {
      case TRANSACTION, TRANSACTION_SUPPORTING_DOCS -> TRANSACTION_DIR_FORMAT + fileId;
      case LOGO -> getBucketName(env, key, fileId, LOGO.name().toLowerCase());
      case INVOICE -> getBucketName(env, key, fileId, INVOICE.name().toLowerCase());
      case ATTACHMENT -> getBucketName(env, key, fileId, ATTACHMENT.name().toLowerCase());
      case AREA_PICTURE -> getBucketName(env, key, fileId, AREA_PICTURE.name().toLowerCase());
      case INVOICE_ZIP -> getBucketName(env, key, fileId, INVOICE_ZIP.name().toLowerCase());
      default -> throw new RuntimeException("Unknown file type " + fileType);
    };
  }

  private String getBucketName(String env, String userS3Path, String fileId, String type) {
    return String.format("%s/accounts/%s/%s/%s", env, userS3Path, type, fileId);
  }

  private String getUserS3Path(String userId) {
    return userRepository.getById(userId).getOldS3key();
  }
}
