package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Bank;
import app.bpartners.api.model.mapper.BankMapper;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.BankJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
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
  private final UserJpaRepository userJpaRepository;
  private final UserMapper userMapper;
  private final BankMapper mapper;
  private final BankJpaRepository jpaRepository;
  private final AccountHolderJpaRepository holderJpaRepository;
  private final AccountJpaRepository accountJpaRepository;

  // TODO: improve this
  @Override
  public Bank findById(String id) {
    if (id == null) {
      return null;
    }
    return mapper.toDomain(jpaRepository.findById(id).orElse(null), null);
  }
}
