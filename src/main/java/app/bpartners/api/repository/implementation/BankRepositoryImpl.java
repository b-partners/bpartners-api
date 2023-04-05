package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.BankMapper;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.repository.bridge.repository.BridgeBankRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
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

  @Override
  public Bank findById(Long id) {
    return mapper.toDomain(bridgeRepository.findById(id));
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
        .bank(findById(connectionChosen.getBankId()))
        .status(savedEntity.getBankConnectionStatus())
        .build();
  }

  //TODO: implement https://docs.bridgeapi.io/reference/refresh-an-item in BridgeAPI
  // Then call it here
  // NOTE : set the token as arg so you can loop it for each user
  @Override
  public BankConnection refreshBankConnection(User user) {
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
