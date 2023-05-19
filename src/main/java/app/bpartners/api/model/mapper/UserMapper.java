package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.IdentificationStatus;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.model.User.CreateBridgeUser;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.swan.model.SwanUser;
import java.util.List;
import java.util.stream.Collectors;
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
  private final AccountHolderMapper accountHolderMapper;

  public User toDomain(HUser entityUser) {
    return User.builder()
        .id(entityUser.getId())
        .firstName(entityUser.getFirstName())
        .lastName(entityUser.getLastName())
        .mobilePhoneNumber(entityUser.getPhoneNumber())
        .email(entityUser.getEmail())
        .accessToken(entityUser.getAccessToken())
        .bridgePassword(entityUser.getBridgePassword())
        .bankConnectionId(entityUser.getBridgeItemId())
        .bridgeItemUpdatedAt(entityUser.getBridgeItemUpdatedAt())
        .bridgeItemLastRefresh(entityUser.getBridgeItemLastRefresh())
        .monthlySubscription(entityUser.getMonthlySubscription())
        .status(entityUser.getStatus())
        .logoFileId(entityUser.getLogoFileId())
        .idVerified(entityUser.getIdVerified())
        .identificationStatus(entityUser.getIdentificationStatus())
        .oldS3key(entityUser.getOldS3AccountKey())
        .accounts(entityUser.getAccounts() == null ? null : entityUser.getAccounts().stream()
            //TODO: map bank as args or through JPA
            //TODO: An user can choose which account to use if more than one
            .map(account -> accountMapper.toDomain(account, null))
            .collect(Collectors.toList()))
        .accountHolders(entityUser.getAccountHolders() == null ? null
            : entityUser.getAccountHolders().stream()
            .map(accountHolderMapper::toDomain)
            .collect(Collectors.toList()))
        .preferredAccountId(entityUser.getPreferredAccountId())
        .externalUserId(entityUser.getBridgeUserId())
        .build();
  }

  public User toDomain(HUser entityUser, SwanUser swanUser) {
    return User.builder()
        .id(entityUser.getId())
        .preferredAccountId(entityUser.getPreferredAccountId())
        .firstName(swanUser == null ? entityUser.getFirstName() : swanUser.getFirstName())
        .lastName(swanUser == null ? entityUser.getLastName() : swanUser.getLastName())
        .mobilePhoneNumber(entityUser.getPhoneNumber())
        .email(entityUser.getEmail())
        .accessToken(entityUser.getAccessToken())
        .bridgePassword(entityUser.getBridgePassword())
        .bankConnectionId(entityUser.getBridgeItemId())
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
        .accounts(entityUser.getAccounts() == null ? null : entityUser.getAccounts().stream()
            //TODO: map bank as args or through JPA
            //TODO: An user can choose which account to use if more than one
            .map(account -> accountMapper.toDomain(account, null))
            .collect(Collectors.toList()))
        .accountHolders(entityUser.getAccountHolders() == null ? null
            : entityUser.getAccountHolders().stream()
            .map(accountHolderMapper::toDomain)
            .collect(Collectors.toList()))
        .externalUserId(swanUser == null ? null : swanUser.getId())
        .oldS3key(entityUser.getOldS3AccountKey())
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
        .bridgeUserId(toSave.getExternalUserId())
        .bridgePassword(toSave.getBridgePassword())
        .identificationStatus(toSave.getIdentificationStatus())
        .status(toSave.getStatus())
        .idVerified(toSave.getIdVerified())
        .oldS3AccountKey(toSave.getOldS3key())
        .build();
  }

  public HUser toEntity(User user, List<HAccountHolder> accountHolders) {
    return toEntity(user).toBuilder()
        .accountHolders(accountHolders)
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
