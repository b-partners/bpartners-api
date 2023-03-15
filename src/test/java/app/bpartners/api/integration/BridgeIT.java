package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.integration.conf.BridgeAbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.User.BridgeUser;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.UserSwanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = BridgeIT.ContextInitializer.class)
@AutoConfigureMockMvc
@Slf4j
public class BridgeIT {
  public static final String ITEM_ID = "7686392";
  @MockBean
  private BuildingPermitConf buildingPermitConf;
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private SwanConf swanConf;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private UserSwanRepository userSwanRepositoryMock;
  @MockBean
  private AccountSwanRepository accountSwanRepositoryMock;
  @MockBean
  private SwanComponent swanComponentMock;
  @Autowired
  private BridgeApi subject;

  public BridgeUser bridgeUser() {
    return BridgeUser.builder()
        .uuid("c2ebbe2c-7804-4d7d-b1b0-77059a0c1519")
        .email("bpartners.artisans@mail.com")
        .build();
  }

  //TODO: do not run this test
//  @Test
//  void read_user_by_id_ok() {
//    BridgeUser actual = subject.findById(bridgeUser().getUuid());
//
//    assertNotNull(actual);
//    assertEquals(bridgeUser(), actual);
//  }

  //TODO: do not skip this test
//  @Test
//  void read_users_ok() {
//    List<BridgeUser> actual = subject.findAllUsers();
//
//    assertFalse(actual.isEmpty());
//    assertTrue(actual.contains(bridgeUser()));
//  }

  //TODO: do not run this test
//  @Test
//  void authenticate_user_ok() {
//    BridgeUserToken accessToken = subject.authenticateUser(CreateBridgeUser.builder()
//        .email(bridgeUser().getEmail())
//        .password("12345678") //TODO
//        .build());
//
//    log.info("Token={}", accessToken);
//    assertNotNull(accessToken);
//  }

  //TODO: do not run this test
//  @Test
//  void create_users_ok() {
//    BridgeUser actual = subject.createUser(CreateBridgeUser.builder()
//        .email("dummy." + randomUUID() + "@email.com")
//        .password("password")
//        .build());
//
//    assertNotNull(actual);
//  }

  //TODO: do not run this test
//  @Test
//  void initiate_user_bank_connection() {
//    String token = "3581737fcda23c123af74298b46cd688dd231f8d-2b277aff-2fe5-46a7-a615-1adeb4d3b56c";
//    String actual = subject.initiateBankConnection(
//        CreateBridgeItem.builder()
//            .prefillEmail(bridgeUser().getEmail())
//            .build(), token);
//
//    log.info("Connect redirect url={}", actual);
//    assertNotNull(actual);
//  }

//  TODO: do not run this test
//  @Test
//  void read_items_ok() {
//    List<BridgeItem> actual = subject.findItemsByToken(userToken());
//
//    log.info("BridgeItems={}", actual);
//    assertFalse(actual.isEmpty());
//  }

  //  TODO: do not run this test
//  @Test
//  void read_item_by_id_ok() {
//    BridgeItem actual = subject.findItemByIdAndToken(ITEM_ID, userToken());
//
//    log.info("BridgeItem={}", actual);
//    assertNotNull(actual);
//  }

  private String userToken() {
    return "dd8c23a8b8dcbffed9cef7a179724c52cb0c5e5c-03ecb5e1-bed6-4a14-89ee-9bfbaff7780e";
  }


  public static class ContextInitializer extends BridgeAbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
