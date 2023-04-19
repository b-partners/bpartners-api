package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.IdentificationStatus;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.model.User.CreateBridgeUser;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.swan.model.SwanUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Component
@AllArgsConstructor
public class UserMapper {
  public static final String VALID_IDENTITY_STATUS = "ValidIdentity";
  public static final String INSUFFICIENT_DOCUMENT_QUALITY_STATUS = "InsufficientDocumentQuality";
  public static final String INVALID_IDENTITY_STATUS = "InvalidIdentity";
  public static final String PROCESSING_STATUS = "Processing";
  public static final String UNINITIATED_STATUS = "Uninitiated";
  private final AccountMapper accountMapper;

  public User toDomain(HUser entityUser, SwanUser swanUser) {
    return User.builder()
        //TODO: check this later
        .preferredAccountId(entityUser.getPreferredAccountExternalId())
        .id(entityUser.getId())
        .firstName(swanUser == null ? entityUser.getFirstName() : swanUser.getFirstName())
        .lastName(swanUser == null ? entityUser.getLastName() : swanUser.getLastName())
        .mobilePhoneNumber(entityUser.getPhoneNumber())
        .email(entityUser.getEmail())
        .accessToken(entityUser.getAccessToken())
        .bridgePassword(entityUser.getBridgePassword())
        .bridgeItemId(entityUser.getBridgeItemId())
        .bridgeItemUpdatedAt(entityUser.getBridgeItemUpdatedAt())
        .bridgeItemLastRefresh(entityUser.getBridgeItemLastRefresh())
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

  public HUser toEntity(User toSave) {
    return HUser.builder()
        .id(toSave.getId())
        .firstName(toSave.getFirstName())
        .lastName(toSave.getLastName())
        .email(toSave.getEmail())
        .phoneNumber(toSave.getMobilePhoneNumber())
        .bridgePassword(toSave.getBridgePassword())
        .build();
  }

  public HUser toEntity(User toSave, BridgeUser bridgeUser) {
    return toEntity(toSave).toBuilder()
        .bridgeUserId(bridgeUser.getUuid())
        .email(bridgeUser.getEmail())
        .build();
  }

  public CreateBridgeUser toBridgeUser(User user) {
    return CreateBridgeUser.builder()
        .email(user.getEmail())
        .password(user.getBridgePassword())
        .build();
  }
}
