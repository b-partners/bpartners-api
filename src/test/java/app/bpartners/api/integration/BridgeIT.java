package app.bpartners.api.integration;

import static org.hibernate.validator.internal.util.Contracts.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import app.bpartners.api.endpoint.SentryConf;
import app.bpartners.api.integration.conf.BridgeAbstractContextInitializer;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.repository.bridge.model.Transaction.BridgeTransaction;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.bridge.model.User.CreateBridgeUser;
import app.bpartners.api.repository.bridge.response.BridgeTokenResponse;
import app.bpartners.api.repository.connectors.account.AccountConnectorRepository;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = BridgeIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Slf4j
@Disabled
// TODO: WARNING ! run these tests locally only
public class BridgeIT {
  @MockBean private PaymentScheduleService paymentScheduleService;
  public static final String ITEM_ID = "7686392";
  public static final Long TRANSACTION_ID = 36000023191568L;
  @MockBean private BuildingPermitConf buildingPermitConf;
  @MockBean private SentryConf sentryConf;
  @MockBean private SendinblueConf sendinblueConf;
  @MockBean private FintectureConf fintectureConf;
  @MockBean private ProjectTokenManager projectTokenManager;
  @MockBean private AccountConnectorRepository accountConnectorRepositoryMock;

  @Autowired private BridgeApi subject;

  public BridgeUser bridgeUser() {
    return BridgeUser.builder()
        .uuid("c2ebbe2c-7804-4d7d-b1b0-77059a0c1519")
        .email("bpartners.artisans@mail.com")
        .build();
  }

  @Test
  void read_user_by_id_ok() {
    BridgeUser actual = subject.findById(bridgeUser().getUuid());

    assertNotNull(actual);
    assertEquals(bridgeUser(), actual);
  }

  @Test
  void read_users_ok() {
    List<BridgeUser> actual = subject.findAllUsers();

    assertFalse(actual.isEmpty());
    assertTrue(actual.contains(bridgeUser()));
  }

  @Test
  void authenticate_user_ok() {
    BridgeTokenResponse accessToken =
        subject.authenticateUser(
            CreateBridgeUser.builder()
                .email(bridgeUser().getEmail())
                .password("12345678") // TODO
                .build());

    log.info("Token={}", accessToken);
    assertNotNull(accessToken);
  }

  //  TODO: do not run this test
  //  @Test
  //  void create_users_ok() {
  //    BridgeUser actual = subject.createUser(CreateBridgeUser.builder()
  //        .email("dummy." + randomUUID() + "@email.com")
  //        .password("password")
  //        .build());
  //
  //    assertNotNull(actual);
  //  }

  //  TODO: do not run this test
  //  @Test
  //  void initiate_user_bank_connection() {
  //    String token =
  // "3581737fcda23c123af74298b46cd688dd231f8d-2b277aff-2fe5-46a7-a615-1adeb4d3b56c";
  //    String actual = subject.initiateBankConnection(
  //        CreateBridgeItem.builder()
  //            .prefillEmail(bridgeUser().getEmail())
  //            .build(), token);
  //
  //    log.info("Connect redirect url={}", actual);
  //    assertNotNull(actual);
  //  }

  @Test
  void read_account_by_id_ok() {
    BridgeAccount actual = subject.findByAccountById(0L, userToken());

    log.info("BridgeAccount ={}", actual);
    assertNotNull(actual);
  }

  @Test
  void read_accounts_ok() {
    List<BridgeAccount> actual = subject.findAccountsByToken(userToken());

    log.info("BridgeAccounts ={}", actual);
    assertFalse(actual.isEmpty());
  }

  @Test
  void read_items_ok() {
    List<BridgeItem> actual = subject.findItemsByToken(userToken());

    log.info("BridgeItems={}", actual);
    assertFalse(actual.isEmpty());
  }

  @Test
  void read_item_by_id_ok() {
    BridgeItem actual = subject.findItemByIdAndToken(ITEM_ID, userToken());

    log.info("BridgeItem={}", actual);
    assertNotNull(actual);
  }

  @Test
  void read_transactions_ok() {
    List<BridgeTransaction> actual = subject.findTransactionsUpdatedByToken(userToken());

    log.info("BridgeTransactions={}", actual);
    assertFalse(actual.isEmpty());
  }

  @Test
  void read_transaction_by_id_ok() {
    BridgeTransaction actual = subject.findTransactionByIdAndToken(TRANSACTION_ID, userToken());

    log.info("BridgeTransaction={}", actual);
    assertNotNull(actual);
  }

  @Test
  void read_banks_ok() {
    List<BridgeBank> actual = subject.findAllBanks();

    log.info("BridgeBanks={}", actual);
    assertFalse(actual.isEmpty());
  }

  @Test
  void read_bank_by_id_ok() {
    BridgeBank actual = subject.findBankById(579L);

    log.info("BridgeBank={}", actual);
    assertNotNull(actual);
  }

  private String userToken() {
    return "7aeb445c2f48a9eb7a9e1d3d81105558c00d0488-fd723599-f559-40a6-a7ea-4d9dd24dc650";
  }

  @Test
  void delete_item_ok() {
    assertTrue(subject.deleteItem(7838784L, userToken()));
  }

  public static class ContextInitializer extends BridgeAbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.findAvailableTcpPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
