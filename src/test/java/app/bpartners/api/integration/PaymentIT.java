package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.CreatePayment;
import app.bpartners.api.endpoint.rest.model.Payment;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = PaymentIT.ContextInitializer.class)
@AutoConfigureMockMvc
class PaymentIT {
  @MockBean private SentryConf sentryConf;
  @MockBean private CognitoComponent cognitoComponentMock;

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, PaymentIT.ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  void setUp() {
    TestUtils.setUpCognito(cognitoComponentMock);
  }

  static Payment payment1() {
    return new Payment()
        .id(TestUtils.PAYMENT1_ID)
        .feeId(TestUtils.FEE1_ID)
        .type(Payment.TypeEnum.CASH)
        .amount(2000)
        .comment("Comment")
        .creationDatetime(Instant.parse("2022-11-08T08:25:24.00Z"));
  }

  static Payment payment2() {
    return new Payment()
        .id(TestUtils.PAYMENT2_ID)
        .feeId(TestUtils.FEE1_ID)
        .type(Payment.TypeEnum.MOBILE_MONEY)
        .amount(3000)
        .comment(null)
        .creationDatetime(Instant.parse("2022-11-10T08:25:25.00Z"));
  }

  static Payment payment4() {
    return new Payment()
        .id(TestUtils.PAYMENT4_ID)
        .feeId(TestUtils.FEE4_ID)
        .type(Payment.TypeEnum.SCHOLARSHIP)
        .amount(5000)
        .comment(null)
        .creationDatetime(Instant.parse("2022-11-12T08:25:26.00Z"));
  }

  static CreatePayment creatablePayment1() {
    return new CreatePayment()
        .type(CreatePayment.TypeEnum.CASH)
        .amount(2000)
        .comment("Comment");
  }

  static CreatePayment creatablePayment2() {
    return new CreatePayment()
        .type(CreatePayment.TypeEnum.MOBILE_MONEY)
        .amount(6000)
        .comment("Comment");
  }

  @Test
  void student_read_ok() throws ApiException {
    ApiClient student1Client = anApiClient(TestUtils.STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    List<Payment> actual = api.getStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE1_ID, 1, 5);

    assertTrue(actual.contains(payment1()));
    assertTrue(actual.contains(payment2()));
  }

  @Test
  void manager_read_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(TestUtils.MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    List<Payment> actual = api.getStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE1_ID, 1, 5);

    assertTrue(actual.contains(payment1()));
    assertTrue(actual.contains(payment2()));
  }

  @Test
  void student_read_ko() {
    ApiClient student1Client = anApiClient(TestUtils.STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    TestUtils.assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getStudentPayments(TestUtils.STUDENT2_ID, TestUtils.FEE3_ID, null, null));
  }

  @Test
  void teacher_read_ko() {
    ApiClient teacher1Client = anApiClient(TestUtils.TEACHER1_TOKEN);
    PayingApi api = new PayingApi(teacher1Client);

    TestUtils.assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.getStudentPayments(TestUtils.STUDENT2_ID, TestUtils.FEE3_ID, null, null));
  }

  @Test
  void manager_write_ok() throws ApiException {
    ApiClient manager1Client = anApiClient(TestUtils.MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);

    List<Payment> actual = api.createStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE3_ID,
        List.of(creatablePayment1()));

    List<Payment> expected = api.getStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE3_ID, 1, 5);
    assertTrue(expected.containsAll(actual));
  }

  @Test
  void teacher_write_ko() {
    ApiClient teacher1Client = anApiClient(TestUtils.TEACHER1_TOKEN);
    PayingApi api = new PayingApi(teacher1Client);

    TestUtils.assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE1_ID, List.of()));

  }

  @Test
  void student_write_ko() {
    ApiClient student1Client = anApiClient(TestUtils.STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);

    TestUtils.assertThrowsApiException(
        "{\"type\":\"403 FORBIDDEN\",\"message\":\"Access is denied\"}",
        () -> api.createStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE1_ID, List.of()));
  }

  @Test
  void manager_write_ko() throws ApiException {
    ApiClient manager1Client = anApiClient(TestUtils.MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);
    List<Payment> expected = api.getStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE3_ID, 1, 5);

    TestUtils.assertThrowsApiException(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\"Payment amount (8000)"
            + " exceeds fee remaining amount (5000)\"}",
        () -> api.createStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE3_ID, List.of(creatablePayment1(),
            creatablePayment2())));

    List<Payment> actual = api.getStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE3_ID, 1, 5);
    assertEquals(0, expected.size());
    assertEquals(expected, actual);
  }

  @Test
  void manager_write_with_some_bad_fields_ko() {
    ApiClient manager1Client = anApiClient(TestUtils.MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);
    CreatePayment toCreate1 = creatablePayment1().amount(null);
    CreatePayment toCreate2 = creatablePayment1().amount(-1);

    ApiException exception1 = assertThrows(ApiException.class,
        () -> api.createStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE1_ID, List.of(toCreate1)));
    ApiException exception2 = assertThrows(ApiException.class,
        () -> api.createStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE1_ID, List.of(toCreate2)));

    String exceptionMessage1 = exception1.getMessage();
    String exceptionMessage2 = exception2.getMessage();
    assertTrue(exceptionMessage1.contains("Amount is mandatory"));
    assertTrue(exceptionMessage2.contains("Amount must be positive"));
  }
}
