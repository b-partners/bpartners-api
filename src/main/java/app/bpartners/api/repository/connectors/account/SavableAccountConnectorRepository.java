package app.bpartners.api.repository.connectors.account;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.model.AccountConnector;
import app.bpartners.api.service.utils.AccountUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static java.util.UUID.randomUUID;

@Repository
@AllArgsConstructor
@Slf4j
public class SavableAccountConnectorRepository implements AccountConnectorRepository {
  private final AccountJpaRepository jpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final AccountMapper mapper;

  private static final String UNSUPPORTED_ERROR_MESSAGE = "Unsupported: only saving methods are!";

  @Override
  public List<AccountConnector> findByBearer(String bearer) {
    throw new NotImplementedException(UNSUPPORTED_ERROR_MESSAGE);
  }

  @Override
  public AccountConnector findById(String id) {
    throw new NotImplementedException(UNSUPPORTED_ERROR_MESSAGE);
  }

  @Override
  public List<AccountConnector> findByUserId(String idUser) {
    throw new NotImplementedException(UNSUPPORTED_ERROR_MESSAGE);
  }

  @Override
  public AccountConnector save(String idUser, AccountConnector accountConnector) {
    if (accountConnector == null) {
      return null;
    }
    HAccount toSave = checkIdentityUpdate(idUser, accountConnector);
    HAccount saved = jpaRepository.save(toSave);
    return mapper.toConnector(saved);
  }

  @Override
  public List<AccountConnector> saveAll(String idUser, List<AccountConnector> accountConnectors) {
    List<HAccount> accountList = accountConnectors.stream()
        .map(accountConnector -> checkIdentityUpdate(idUser, accountConnector))
        .collect(Collectors.toList());
    return jpaRepository.saveAll(accountList).stream()
        .map(mapper::toConnector)
        .collect(Collectors.toList());
  }

  private HAccount checkIdentityUpdate(String idUser, AccountConnector accountConnector) {
    HUser associatedUser = associatedUser(idUser);
    Optional<HAccount> optionalAccountByExternalId =
        AccountUtils.findByExternalId(accountConnector.getId(), jpaRepository);

    HAccount existingAccount =
        optionalAccountByExternalId.orElseGet(() -> {
          Optional<HAccount> optionalAccountByIban =
              AccountUtils.findByIbanOrNameAndBank(accountConnector, jpaRepository);
          if (optionalAccountByIban.isEmpty()) {
            return fromNewAccount(accountConnector, associatedUser);
          } else {
            HAccount accountSameIban = optionalAccountByIban.get();
            return accountSameIban.toBuilder()
                .enableStatus(EnableStatus.ENABLED)
                .build();
          }
        });
    return mapper.toEntity(accountConnector, existingAccount);
  }

  private HAccount fromNewAccount(AccountConnector accountConnector, HUser user) {
    return HAccount.builder()
        .id(String.valueOf(randomUUID()))
        .name(accountConnector.getName())
        .iban(accountConnector.getIban())
        .user(user)
        .availableBalance(accountConnector.getBalance().stringValue())
        .externalId(accountConnector.getId())
        .idBank(accountConnector.getBankId())
        .enableStatus(EnableStatus.ENABLED)
        .build();
  }

  private HUser associatedUser(String idUser) {
    return idUser == null ? null : userJpaRepository.findById(idUser)
        .orElseThrow(
            () -> new NotFoundException("User(id=" + idUser + " not found"));
  }
}
