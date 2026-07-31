package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.security.model.Role;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HUser;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class UserMapper {
  private final AccountMapper accountMapper;
  private final AccountHolderMapper accountHolderMapper;
  private final UserApiKeyMapper analysisApiKeyMapper;

  public User toDomain(HUser entityUser) {
    var userAnalysisApiKeys =
        entityUser.getAnalysisApiKeys().stream()
            .map(entity -> analysisApiKeyMapper.toDomain(entity, null))
            .toList();
    var user =
        User.builder()
            .id(entityUser.getId())
            .userSubscriptionId(entityUser.getUserSubscriptionE2Id())
            .firstName(entityUser.getFirstName())
            .lastName(entityUser.getLastName())
            .mobilePhoneNumber(entityUser.getPhoneNumber())
            .email(entityUser.getEmail())
            .apiKey(entityUser.getApiKey())
            .status(entityUser.getStatus())
            .logoFileId(entityUser.getLogoFileId())
            .oldS3key(entityUser.getOldS3AccountKey())
            .accounts(
                entityUser.getAccounts() == null
                    ? null
                    : entityUser.getAccounts().stream()
                        .map(accountMapper::toDomain)
                        .collect(Collectors.toList()))
            .accountHolders(
                entityUser.getAccountHolders() == null
                    ? null
                    : entityUser.getAccountHolders().stream()
                        .map(accountHolderMapper::toDomain)
                        .collect(Collectors.toList()))
            .preferredAccountId(entityUser.getPreferredAccountId())
            .roles(entityUser.getRoles() == null ? List.of() : Arrays.asList(entityUser.getRoles()))
            .parentUser(
                entityUser.getParentUser() == null ? null : toDomain(entityUser.getParentUser()))
            .analysisApiKeys(userAnalysisApiKeys)
            .subscriptionProducts(
                entityUser.getUserSubscriptionProducts() == null
                    ? List.of()
                    : entityUser.getUserSubscriptionProducts())
            .build();
    user.addUserAnalysisApiKey(userAnalysisApiKeys);
    return user;
  }

  public HUser toEntity(User toSave) {
    return HUser.builder()
        .id(toSave.getId())
        .userSubscriptionE2Id(toSave.getUserSubscriptionId())
        .firstName(toSave.getFirstName())
        .lastName(toSave.getLastName())
        .email(toSave.getEmail())
        .phoneNumber(toSave.getMobilePhoneNumber())
        .status(toSave.getStatus())
        .apiKey(toSave.getApiKey())
        .oldS3AccountKey(toSave.getOldS3key())
        .preferredAccountId(toSave.getPreferredAccountId())
        .logoFileId(toSave.getLogoFileId())
        .roles(toSave.getRoles() == null ? new Role[] {} : toSave.getRoles().toArray(Role[]::new))
        .parentUser(toSave.getParentUser() == null ? null : toEntity(toSave.getParentUser()))
        .analysisApiKeys(
            toSave.getAnalysisApiKeys().stream().map(analysisApiKeyMapper::toEntity).toList())
        .build();
  }

  public HUser toEntity(User user, List<HAccountHolder> accountHolders, List<HAccount> accounts) {
    return toEntity(user).toBuilder().accountHolders(accountHolders).accounts(accounts).build();
  }
}
