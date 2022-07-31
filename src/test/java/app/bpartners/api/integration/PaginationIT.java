package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.api.UsersApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Fee;
import app.bpartners.api.endpoint.rest.model.Payment;
import app.bpartners.api.endpoint.rest.model.Student;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import software.amazon.awssdk.services.eventbridge.EventBridgeClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = PaginationIT.ContextInitializer.class)
@AutoConfigureMockMvc
class PaginationIT {

  @MockBean
  private SentryConf sentryConf;

  @MockBean
  private CognitoComponent cognitoComponentMock;

  @MockBean
  private EventBridgeClient eventBridgeClientMock;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  @BeforeEach
  public void setUp() throws ApiException {
    TestUtils.setUpCognito(cognitoComponentMock);
    TestUtils.setUpEventBridge(eventBridgeClientMock);
  }

  private void someCreatableStudentList(int nbOfNewStudents) throws ApiException {
    List<Student> newStudents = new ArrayList<>();
    for (int i = 0; i < nbOfNewStudents; i++) {
      newStudents.add(StudentIT.someCreatableStudent());
    }
    ApiClient manager1Client = anApiClient(TestUtils.MANAGER1_TOKEN);
    UsersApi api = new UsersApi(manager1Client);
    api.createOrUpdateStudents(newStudents);
  }

  private static Payment payment2() {
    return new Payment()
        .id("payment2_id")
        .feeId(TestUtils.FEE1_ID)
        .type(Payment.TypeEnum.CASH)
        .amount(3000)
        .comment("Comment")
        .creationDatetime(Instant.parse("2022-11-09T08:25:25.00Z"));
  }

  @Test
  void student_pages_are_ordered_by_reference() throws ApiException {
    someCreatableStudentList(7);
    int pageSize = 4;
    ApiClient teacher1Client = anApiClient(TestUtils.TEACHER1_TOKEN);
    UsersApi api = new UsersApi(teacher1Client);

    final List<Student> page1 = api.getStudents(1, pageSize, null, null, null);
    final List<Student> page2 = api.getStudents(2, pageSize, null, null, null);
    final List<Student> page3 = api.getStudents(3, pageSize, null, null, null);
    final List<Student> page4 = api.getStudents(4, pageSize, null, null, null);
    final List<Student> page100 = api.getStudents(100, pageSize, null, null, null);

    assertEquals(pageSize, page1.size());
    assertEquals(pageSize, page2.size());
    assertEquals(2, page3.size());
    assertEquals(0, page4.size());
    assertEquals(0, page100.size());
    // students are ordered by ref
    assertTrue(isBefore(page1.get(0).getRef(), page1.get(2).getRef()));
    assertTrue(isBefore(page1.get(2).getRef(), page2.get(0).getRef()));
    assertTrue(isBefore(page2.get(0).getRef(), page2.get(2).getRef()));
  }

  @Test
  void fees_pages_are_ordered_by_due_datetime_desc() throws ApiException {
    ApiClient student1Client = anApiClient(TestUtils.STUDENT1_TOKEN);
    PayingApi api = new PayingApi(student1Client);
    int pageSize = 2;

    List<Fee> page1 = api.getStudentFees(TestUtils.STUDENT1_ID, 1, pageSize);
    List<Fee> page2 = api.getStudentFees(TestUtils.STUDENT1_ID, 2, pageSize);
    List<Fee> page3 = api.getStudentFees(TestUtils.STUDENT1_ID, 3, pageSize);

    assertEquals(pageSize, page1.size());
    assertEquals(1, page2.size());
    assertEquals(0, page3.size());
    assertTrue(isAfter(page1.get(0).getDueDatetime(), page1.get(1).getDueDatetime()));
    assertTrue(isAfter(page1.get(1).getDueDatetime(), page2.get(0).getDueDatetime()));
  }

  @Test
  void payments_pages_are_ordered_by_due_datetime_desc() throws ApiException {
    ApiClient manager1Client = anApiClient(TestUtils.MANAGER1_TOKEN);
    PayingApi api = new PayingApi(manager1Client);
    int pageSize = 2;

    List<Payment> page1 = api.getStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE1_ID, 1, pageSize);
    List<Payment> page2 = api.getStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE1_ID, 2, pageSize);
    List<Payment> page3 = api.getStudentPayments(TestUtils.STUDENT1_ID, TestUtils.FEE1_ID, 3, pageSize);

    assertEquals(pageSize, page1.size());
    assertEquals(1, page2.size());
    assertEquals(0, page3.size());
    assertTrue(isAfter(page1.get(0).getCreationDatetime(), page1.get(1).getCreationDatetime()));
    assertTrue(isAfter(page1.get(1).getCreationDatetime(), page2.get(0).getCreationDatetime()));
  }

  private boolean isBefore(String a, String b) {
    return a.compareTo(b) < 0;
  }

  private boolean isAfter(Instant a, Instant b) {
    return a.compareTo(b) > 0;
  }

  @Test
  void page_parameters_are_validated() {
    ApiClient teacher1Client = anApiClient(TestUtils.TEACHER1_TOKEN);

    UsersApi api = new UsersApi(teacher1Client);
    TestUtils.assertThrowsApiException(
            "{\"type\":\"400 BAD_REQUEST\",\"message\":\"page value must be >=1\"}",
            () -> api.getStudents(0, 20, null, null, null));
    TestUtils.assertThrowsApiException(
            "{\"type\":\"400 BAD_REQUEST\",\"message\":\"page size must be <500\"}",
            () -> api.getStudents(1, 1000, null, null, null));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
