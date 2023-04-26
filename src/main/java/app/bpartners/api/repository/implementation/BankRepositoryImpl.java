package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.BankMapper;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.repository.bridge.repository.BridgeBankRepository;
import app.bpartners.api.repository.jpa.BankJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HBank;
import app.bpartners.api.repository.jpa.model.HUser;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.model.BankConnection.BankConnectionStatus.NOT_SUPPORTED;
import static app.bpartners.api.model.BankConnection.BankConnectionStatus.OK;
import static app.bpartners.api.model.BankConnection.BankConnectionStatus.UNKNOWN;
import static app.bpartners.api.model.BankConnection.BankConnectionStatus.VALIDATION_REQUIRED;

@Repository
@Slf4j
@AllArgsConstructor
public class BankRepositoryImpl implements BankRepository {
  public static final int ITEM_STATUS_OK = 0;
  public static final int ITEM_STATUS_NOT_SUPPORTED = 1005;
  public static final int ITEM_STATUS_PRO = 1100;
  private final BridgeBankRepository bridgeRepository;
  private final UserJpaRepository userJpaRepository;
  private final UserMapper userMapper;
  private final BankMapper mapper;
  private final BankJpaRepository jpaRepository;

  //TODO: check why bank is persisted twice and turn back to optional
  @Override
  public Bank findByBridgeId(Long id) {
    BridgeBank bridgeBank = bridgeRepository.findById(id);

    /* TODO: pre-condition check first, nominal execution afterwards
     *   That is: if (bridgeBank == null) {throw} first */
    if (bridgeBank != null) {
      List<HBank> entities = jpaRepository.findAllByBridgeId(bridgeBank.getId());
      HBank entity;
      if (entities.isEmpty()) {
        entity = jpaRepository.save(mapper.toEntity(bridgeBank));
      } else {
        entity = entities.get(0);
      }
      return mapper.toDomain(entity, bridgeBank);
    }
    throw new NotFoundException("Bank(bridgeId=" + id + " is not found");
  }

  //TODO: improve this
  @Override
  public Bank findById(String id) {
    if (id == null) {
      return null;
    }
    return mapper.toDomain(
        jpaRepository.findById(id).orElse(null), null);
  }

  @Override
  public BankConnection selfUpdateBankConnection() {
    List<BridgeItem> bridgeItems = bridgeRepository.getBridgeItems();
    if (bridgeItems.isEmpty()) {
      return null;
    }
    BridgeItem connectionChosen = bridgeRepository.getBridgeItems().get(0);
    if (bridgeItems.size() > 2) {
      log.warn("[Bridge] Only one bank connection supported for now. "
          + "Therefore these connections are found :" + bridgeItems);
    }
    HUser userToUpdate =
        userJpaRepository.getById(AuthProvider.getPrincipal().getUser().getId()).toBuilder()
            .bridgeItemId(connectionChosen.getId())
            .bankConnectionStatus(getStatus(connectionChosen.getStatus()))
            .bridgeItemUpdatedAt(Instant.now())
            .build();
    HUser savedEntity = userJpaRepository.save(userToUpdate);
    return BankConnection.builder()
        .bridgeId(savedEntity.getBridgeItemId())
        .user(userMapper.toDomain(savedEntity, null))
        .bank(findByBridgeId(connectionChosen.getBankId()))
        .status(savedEntity.getBankConnectionStatus())
        .build();
  }

  @Override
  public Instant refreshBankConnection(UserToken userToken) {
    if (userToken == null || userToken.getUser().getBridgeItemId() == null) {
      return null;
    }
    User user = userToken.getUser();
    if (bridgeRepository.refreshBankConnection(
        user.getBridgeItemId(), userToken.getAccessToken()) != null) {
      Instant refreshedAt =
          bridgeRepository.getItemStatusRefreshedAt(
              user.getBridgeItemId(), userToken.getAccessToken());
      HUser userEntity = userJpaRepository.getById(user.getId());
      if (userEntity.getBridgeItemLastRefresh() != null
          && userEntity.getBridgeItemLastRefresh().equals(refreshedAt)) {
        //Do not update item last refresh instant
        return null;
      }
      return userJpaRepository.save(
              userEntity.toBuilder()
                  .bridgeItemLastRefresh(refreshedAt)
                  .build())
          .getBridgeItemLastRefresh();
    }
    return null;
  }

  public static BankConnection.BankConnectionStatus getStatus(Integer statusValue) {
    switch (statusValue) {
      case ITEM_STATUS_OK:
        return OK;
      case ITEM_STATUS_NOT_SUPPORTED:
        return NOT_SUPPORTED;
      case ITEM_STATUS_PRO:
        return VALIDATION_REQUIRED;
      default:
        return UNKNOWN;
    }
  }
}
