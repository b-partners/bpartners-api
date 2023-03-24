package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.IdentificationStatus;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.swan.model.SwanUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Component
@AllArgsConstructor
public class UserMapper {
  private final AccountMapper accountMapper;
  public static final String VALID_IDENTITY_STATUS = "ValidIdentity";
  public static final String INSUFFICIENT_DOCUMENT_QUALITY_STATUS = "InsufficientDocumentQuality";
  public static final String INVALID_IDENTITY_STATUS = "InvalidIdentity";
  public static final String PROCESSING_STATUS = "Processing";
  public static final String UNINITIATED_STATUS = "Uninitiated";

  public User toDomain(HUser entityUser, SwanUser swanUser) {
    return User.builder()
        .id(entityUser.getId())
        .firstName(swanUser == null ? entityUser.getFirstName() : swanUser.getFirstName())
        .lastName(swanUser == null ? entityUser.getLastName() : swanUser.getLastName())
        .mobilePhoneNumber(entityUser.getPhoneNumber())
        .email(entityUser.getEmail())
        .bridgePassword(entityUser.getBridgePassword())
        .monthlySubscription(entityUser.getMonthlySubscription())
        .status(entityUser.getStatus())
        .logoFileId(entityUser.getLogoFileId())
        .idVerified(swanUser == null ? entityUser.getIdVerified() : swanUser.isIdVerified())
        .identificationStatus(
            swanUser == null ? entityUser.getIdentificationStatus()
                : getIdentificationStatus(
                swanUser.getIdentificationStatus()))
        .account(entityUser.getAccounts() == null || entityUser.getAccounts().isEmpty() ? null
            : accountMapper.toDomain(entityUser.getAccounts().get(0),
            entityUser.getId()))
        .build();
  }

  public IdentificationStatus getIdentificationStatus(String value) {
    switch (value) {
      case VALID_IDENTITY_STATUS:
        return IdentificationStatus.VALID_IDENTITY;
      case INSUFFICIENT_DOCUMENT_QUALITY_STATUS:
        return IdentificationStatus.INSUFFICIENT_DOCUMENT_QUALITY;
      case INVALID_IDENTITY_STATUS:
        return IdentificationStatus.INVALID_IDENTITY;
      case PROCESSING_STATUS:
        return IdentificationStatus.PROCESSING;
      case UNINITIATED_STATUS:
        return IdentificationStatus.UNINITIATED;
      default:
        throw new ApiException(SERVER_EXCEPTION, "Unknown identification status : " + value);
    }
  }
}
