package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.model.AccountConnector;
import java.util.List;
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
  public List<AccountConnector> findByUserId(String userId) {
    throw new NotImplementedException(UNSUPPORTED_ERROR_MESSAGE);
  }

  @Override
  public AccountConnector save(AccountConnector accountConnector) {
    if (accountConnector == null) {
      return null;
    }
    HAccount toSave = mapper.toEntity(accountConnector,
        jpaRepository.findByExternalId(accountConnector.getId())
            .orElse(fromNewAccount(accountConnector)));
    HAccount saved = jpaRepository.save(toSave);
    return mapper.toConnector(saved);
  }

  @Override
  public List<AccountConnector> saveAll(List<AccountConnector> accountConnectors) {
    List<HAccount> toSave = accountConnectors.stream()
        .map(accountConnector -> mapper.toEntity(accountConnector,
            jpaRepository.findByExternalId(accountConnector.getId())
                .orElse(fromNewAccount(accountConnector))))
        .collect(Collectors.toList());
    return jpaRepository.saveAll(toSave).stream()
        .map(mapper::toConnector)
        .collect(Collectors.toList());
  }

  private static HAccount fromNewAccount(AccountConnector accountConnector) {
    return HAccount.builder()
        .id(String.valueOf(randomUUID()))
        .externalId(accountConnector.getId())
        .idBank(accountConnector.getBankId())
        .build();
  }
}
