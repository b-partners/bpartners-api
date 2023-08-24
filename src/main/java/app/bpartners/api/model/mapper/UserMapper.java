package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.IdentificationStatus;
import app.bpartners.api.endpoint.rest.security.model.Role;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.model.User.CreateBridgeUser;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HUser;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Component
@AllArgsConstructor
@Slf4j
public class UserMapper {
  public static final String VALID_IDENTITY_STATUS = "ValidIdentity";
  public static final String INSUFFICIENT_DOCUMENT_QUALITY_STATUS = "InsufficientDocumentQuality";
  public static final String INVALID_IDENTITY_STATUS = "InvalidIdentity";
  public static final String PROCESSING_STATUS = "Processing";
  public static final String UNINITIATED_STATUS = "Uninitiated";
  private final AccountMapper accountMapper;
  private final AccountHolderMapper accountHolderMapper;

  public User toDomain(HUser entity, Bank bank) {
    return toDomain(entity).toBuilder()
        .accounts(entity.getAccounts() == null ? null : entity.getAccounts().stream()
            .map(account -> accountMapper.toDomain(account, bank))
            .collect(Collectors.toList()))
        .build();
  }

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
            .map(account -> accountMapper.toDomain(account, null))
            .collect(Collectors.toList()))
        .accountHolders(entityUser.getAccountHolders() == null ? null
            : entityUser.getAccountHolders().stream()
            .map(accountHolderMapper::toDomain)
            .collect(Collectors.toList()))
        .preferredAccountId(entityUser.getPreferredAccountId())
        .externalUserId(entityUser.getBridgeUserId())
        .connectionStatus(entityUser.getBankConnectionStatus())
        .roles(entityUser.getRoles() == null ? List.of()
            : Arrays.asList(entityUser.getRoles()))
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
        .preferredAccountId(toSave.getPreferredAccountId())
        .bridgeItemUpdatedAt(toSave.getBridgeItemUpdatedAt())
        .bridgeItemLastRefresh(toSave.getBridgeItemLastRefresh())
        .bankConnectionStatus(toSave.getConnectionStatus())
        .bridgeItemId(toSave.getBankConnectionId())
        .logoFileId(toSave.getLogoFileId())
        .monthlySubscription(toSave.getMonthlySubscription())
        .roles(toSave.getRoles() == null ? new Role[] {}
            : toSave.getRoles().toArray(Role[]::new))
        .build();
  }

  public HUser toEntity(User user, List<HAccountHolder> accountHolders, List<HAccount> accounts) {
    return toEntity(user).toBuilder()
        .accountHolders(accountHolders)
        .accounts(accounts)
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
