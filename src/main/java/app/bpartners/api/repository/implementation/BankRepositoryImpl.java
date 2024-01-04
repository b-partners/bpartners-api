package app.bpartners.api.repository.implementation;

import static app.bpartners.api.model.BankConnection.BankConnectionStatus.INVALID_CREDENTIALS;
import static app.bpartners.api.model.BankConnection.BankConnectionStatus.NOT_SUPPORTED;
import static app.bpartners.api.model.BankConnection.BankConnectionStatus.OK;
import static app.bpartners.api.model.BankConnection.BankConnectionStatus.SCA_REQUIRED;
import static app.bpartners.api.model.BankConnection.BankConnectionStatus.UNDERGOING_REFRESHMENT;
import static app.bpartners.api.model.BankConnection.BankConnectionStatus.UNKNOWN;
import static app.bpartners.api.model.BankConnection.BankConnectionStatus.VALIDATION_REQUIRED;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.mapper.BankMapper;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.UserTokenRepository;
import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.repository.bridge.repository.BridgeBankRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.BankJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.jpa.model.HBank;
import app.bpartners.api.repository.jpa.model.HUser;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@AllArgsConstructor
public class BankRepositoryImpl implements BankRepository {
  public static final int ITEM_STATUS_OK = 0;
  public static final int ITEM_STATUS_NOT_SUPPORTED = 1005;
  public static final int ITEM_STATUS_PRO = 1100;
  public static final int ITEM_STATUS_INVALID_CREDENTIALS = 402;
  public static final int ITEM_STATUS_SCA_REQUIRED = 1010;
  public static final int TRY_AGAIN = 1003;
  public static final int BRIDGE_ACCOUNT_ADDED_RECENTLY = -2;
  public static final int BRIDGE_CREDENTIAL_UPDATED_RECENTLY = -3;
  private final BridgeBankRepository bridgeRepository;
  private final UserJpaRepository userJpaRepository;
  private final UserMapper userMapper;
  private final BankMapper mapper;
  private final BankJpaRepository jpaRepository;
  private final UserTokenRepository userTokenRepository;
  private final AccountHolderJpaRepository holderJpaRepository;
  private final AccountJpaRepository accountJpaRepository;

  public static BankConnection.BankConnectionStatus getBankConnectionStatus(Integer statusValue) {
    switch (statusValue) {
      case ITEM_STATUS_OK:
        return OK;
      case ITEM_STATUS_NOT_SUPPORTED:
        return NOT_SUPPORTED;
      case ITEM_STATUS_PRO:
        return VALIDATION_REQUIRED;
      case ITEM_STATUS_INVALID_CREDENTIALS:
        return INVALID_CREDENTIALS;
      case ITEM_STATUS_SCA_REQUIRED:
        return SCA_REQUIRED;
      case TRY_AGAIN:
        return BankConnection.BankConnectionStatus.TRY_AGAIN;
      case BRIDGE_ACCOUNT_ADDED_RECENTLY:
      case BRIDGE_CREDENTIAL_UPDATED_RECENTLY:
        return UNDERGOING_REFRESHMENT;
      default:
        log.warn("Unknown bank status " + statusValue);
        return UNKNOWN;
    }
  }

  // TODO: check if it is necessary to persist values
  @Override
  public String initiateConnection(User user) {
    return bridgeRepository.initiateBankConnection(user.getEmail());
  }

  // TODO: check why bank is persisted twice and turn back to optional
  @Override
  public Bank findByExternalId(String id) {
    if (id == null || bridgeRepository.findById(Long.valueOf(id)) == null) {
      return null;
    }
    BridgeBank bridgeBank = bridgeRepository.findById(Long.valueOf(id));
    List<HBank> entities = jpaRepository.findAllByExternalId(bridgeBank.getId());
    HBank entity;
    if (entities.isEmpty()) {
      entity = jpaRepository.save(mapper.toEntity(bridgeBank));
    } else {
      entity = entities.get(0);
    }
    return mapper.toDomain(entity, bridgeBank);
  }

  // TODO: improve this
  @Override
  public Bank findById(String id) {
    if (id == null) {
      return null;
    }
    return mapper.toDomain(jpaRepository.findById(id).orElse(null), null);
  }

  @Override
  public BankConnection updateBankConnection(User user) {
    List<BridgeItem> bridgeItems = bridgeRepository.getBridgeItems();
    if (bridgeItems.isEmpty()) {
      return null;
    }
    BridgeItem connectionChosen = bridgeItems.get(0);
    if (bridgeItems.size() > 1) {
      log.warn(
          "[Bridge] Only one bank connection supported for now. "
              + "Therefore these connections are found :"
              + bridgeItems);
    }
    List<HAccountHolder> accountHolders = holderJpaRepository.findAllByIdUser(user.getId());
    List<HAccount> accounts = accountJpaRepository.findByUser_Id(user.getId());
    HUser entityToSave =
        userMapper.toEntity(user, accountHolders, accounts).toBuilder()
            .bridgeItemId(connectionChosen.getId())
            .bankConnectionStatus(getBankConnectionStatus(connectionChosen.getStatus()))
            .bridgeItemUpdatedAt(Instant.now())
            .build();
    HUser savedEntity = userJpaRepository.save(entityToSave);

    Bank bank = findByExternalId(String.valueOf(connectionChosen.getBankId()));
    return BankConnection.builder()
        .bridgeId(savedEntity.getBridgeItemId())
        .user(userMapper.toDomain(savedEntity, bank))
        .bank(bank)
        .status(savedEntity.getBankConnectionStatus())
        .build();
  }

  @Override
  public Instant refreshBankConnection(UserToken userToken) {
    if (userToken == null || userToken.getUser().getBankConnectionId() == null) {
      return null;
    }
    User user = userToken.getUser();
    if (bridgeRepository.refreshBankConnection(
            user.getBankConnectionId(), userToken.getAccessToken())
        != null) {
      Instant refreshedAt =
          bridgeRepository.getItemStatusRefreshedAt(
              user.getBankConnectionId(), userToken.getAccessToken());
      HUser userEntity = userJpaRepository.getById(user.getId());
      if (userEntity.getBridgeItemLastRefresh() != null
          && userEntity.getBridgeItemLastRefresh().equals(refreshedAt)) {
        // Do not update item last refresh instant
        return null;
      }
      return userJpaRepository
          .save(userEntity.toBuilder().bridgeItemLastRefresh(refreshedAt).build())
          .getBridgeItemLastRefresh();
    }
    return null;
  }

  @Override
  public String initiateProValidation(String accountId) {
    UserToken userToken = userTokenRepository.getLatestTokenByAccount(accountId);
    return bridgeRepository.validateCurrentProItems(userToken.getAccessToken()).getRedirectUrl();
  }

  @Override
  public String initiateBankConnectionEdition(Account account) {
    BridgeItem defaultItem = getDefaultItem(account);
    return bridgeRepository.editItem(defaultItem.getId()).getRedirectUrl();
  }

  @Override
  public String initiateScaSync(Account account) {
    BridgeItem defaultItem = getDefaultItem(account);
    return bridgeRepository.synchronizeSca(defaultItem.getId()).getRedirectUrl();
  }

  @Override
  public boolean disconnectBank(User user) {
    return bridgeRepository.deleteItem(user.getBankConnectionId(), user.getAccessToken());
  }

  private BridgeItem getDefaultItem(Account account) {
    // TODO: item should be retrieved from HAccount not from Bridge
    List<BridgeItem> items = bridgeRepository.getBridgeItems();
    BridgeItem defaultItem = items.get(0);
    if (items.size() > 1) {
      log.warn(
          "[Bridge] Multiple items ("
              + items
              + ")  found for"
              + account.describeInfos()
              + "."
              + defaultItem.toString()
              + "chosen by default)");
    }
    return defaultItem;
  }
}
