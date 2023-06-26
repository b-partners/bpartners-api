package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.IdentificationStatus;
import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.model.mapper.UserTokenMapper;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.model.User.CreateBridgeUser;
import app.bpartners.api.repository.bridge.response.BridgeTokenResponse;
import app.bpartners.api.repository.implementation.UserTokenRepositoryImpl;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import java.math.BigInteger;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserTokenRepositoryImplTest {
  UserMapper userMapper;
  UserJpaRepository userJpaRepository;
  UserTokenMapper userTokenMapper;
  BridgeApi bridgeApi;
  AccountHolderJpaRepository holderJpaRepository;
  AccountJpaRepository accountJpaRepository;
  UserTokenRepositoryImpl subject;

  @BeforeEach
  void setUp() {
    userJpaRepository = mock(UserJpaRepository.class);
    userTokenMapper = mock(UserTokenMapper.class);
    bridgeApi = mock(BridgeApi.class);
    userMapper = mock(UserMapper.class);
    accountJpaRepository = mock(AccountJpaRepository.class);
    holderJpaRepository = mock(AccountHolderJpaRepository.class);
    subject = new UserTokenRepositoryImpl(
        userJpaRepository, userTokenMapper, bridgeApi, userMapper,
        accountJpaRepository, holderJpaRepository);

    when(userTokenMapper.toBridgeAuthUser(any())).thenReturn(bridgeUser());
    when(bridgeApi.authenticateUser(any())).thenReturn(
        BridgeTokenResponse.builder()
            .user(BridgeUser.builder()
                .email(user().getEmail())
                .build())
            .accessToken(user().getAccessToken())
            .build());
    when(userTokenMapper.toDomain(any())).thenReturn(UserToken.builder()
        .user(user())
        .accessToken(user().getAccessToken())
        .build());
    when(userMapper.toEntity(any(), any(), any())).thenReturn(HUser.builder().build());
    when(userJpaRepository.getHUserById(any())).thenReturn(entity());
    when(userJpaRepository.save(any())).thenReturn(entity());
  }

  @Test
  void update_user_token_ok() {
    UserToken actual = subject.updateUserToken(user());

    assertNotNull(actual);
  }

  @Test
  void read_latest_token_ok() {
    UserToken actual = subject.getLatestTokenByUser(user());

    assertNotNull(actual.getAccessToken());
  }

  HUser entity() {
    return HUser.builder()
        .accessToken(JOE_DOE_TOKEN)
        .tokenCreationDatetime(Instant.now())
        .tokenExpirationDatetime(Instant.now().plus(1L, ChronoUnit.DAYS))
        .build();
  }

  User user() {
    return User.builder()
        .id(JOE_DOE_ID)
        .logoFileId("logo_id")
        .firstName("joe")
        .lastName("doe")
        .email("exemple@gmail.com")
        .bridgePassword("password")
        .mobilePhoneNumber("+3312345678")
        .bankConnectionId(1L)
        .bridgeItemUpdatedAt(Instant.parse("2023-03-30T13:35:26.853Z"))
        .bridgeItemLastRefresh(Instant.parse("2023-04-30T13:35:26.853Z"))
        .accessToken(JOE_DOE_TOKEN)
        .monthlySubscription(1)
        .status(EnableStatus.ENABLED)
        .idVerified(true)
        .identificationStatus(IdentificationStatus.VALID_IDENTITY)
        .accountHolders(List.of(accountHolder()))
        .accounts(List.of(account()))
        .preferredAccountId("account_id")
        .externalUserId("user_id")
        .oldS3key(null)
        .build();
  }

  private static Account account() {
    return Account.builder()
        .id("account_id")
        .externalId("1234")
        .idAccountHolder("account_holder_id")
        .userId(JOE_DOE_ID)
        .name("joe doe")
        .iban("iban")
        .bic("bic")
        .availableBalance(new Fraction(BigInteger.ZERO, BigInteger.ONE))
        .bank(Bank.builder()
            .id("bank_id")
            .externalId(1234L)
            .name("bank")
            .logoUrl("http://logo.url")
            .build())
        .active(true)
        .status(AccountStatus.OPENED)
        .build();
  }

  private static AccountHolder accountHolder() {
    return AccountHolder.builder()
        .id("account_holder_id")
        .userId(JOE_DOE_ID)
        .name("joe")
        .address("address")
        .city("city")
        .country("country")
        .postalCode("123")
        .socialCapital(1)
        .vatNumber("123")
        .siren("Siren")
        .mainActivity("activity")
        .mainActivityDescription("description")
        .mobilePhoneNumber("+3312345667")
        .email("exemple@gmail.com")
        .initialCashflow(new Fraction(BigInteger.ZERO, BigInteger.ONE))
        .feedbackLink(null)
        .subjectToVat(true)
        .verificationStatus(VerificationStatus.VERIFIED)
        .location(new Geojson())
        .townCode(1)
        .prospectingPerimeter(1)
        .build();
  }

  CreateBridgeUser bridgeUser() {
    return CreateBridgeUser.builder()
        .email(user().getEmail())
        .password("password")
        .build();
  }
}
