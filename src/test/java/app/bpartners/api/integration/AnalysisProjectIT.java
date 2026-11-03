package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.AnalysisProjectApi;
import app.bpartners.api.endpoint.rest.api.DetectionTrackingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AnalysisProjectStep;
import app.bpartners.api.endpoint.rest.model.CreateDetectionTracking;
import app.bpartners.api.endpoint.rest.model.DetectionInitiator;
import app.bpartners.api.endpoint.rest.model.RegisterAnalysis2d;
import app.bpartners.api.endpoint.rest.model.RegisterModel3d;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.User;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.AnalysisProjectRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.DetectionTrackingJpaRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.credit.CreditService;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class AnalysisProjectIT extends MockedThirdParties {
  private final String dummyApiKey = "dummyApiKey";
  @Autowired private CreditTransactionRepository creditTransactionRepository;
  @Autowired private DetectionTrackingJpaRepository detectionTrackingJpaRepository;
  @Autowired private AnalysisProjectRepository analysisProjectRepository;
  @Autowired private UserSubscriptionEligibleJpaRepository userSubscriptionEligibleRepository;
  @Autowired private CreditService creditService;
  @MockBean private UserRepository userRepository;
  final User userMock = mock(User.class);

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(null, dummyApiKey, localPort);
  }

  @BeforeEach
  void setUp() {
    when(userMock.getId()).thenReturn(JOE_DOE_ID);
    when(userMock.getEmail()).thenReturn(JOE_EMAIL);
    when(userRepository.findByApiKey(dummyApiKey)).thenReturn(Optional.of(userMock));
    when(userRepository.getByEmail(JOE_EMAIL)).thenReturn(userMock);
    when(userRepository.getById(JOE_DOE_ID)).thenReturn(userMock);

    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpUserSubscription(subscriptionService);
    when(subscriptionService.addConsumption(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    clearJoeDoeCreditContext();
    grantJoeDoe(100L);
  }

  @AfterEach
  void tearDown() {
    clearJoeDoeCreditContext();
  }

  private void clearJoeDoeCreditContext() {
    userSubscriptionEligibleRepository
        .findByUserId(JOE_DOE_ID)
        .ifPresent(userSubscriptionEligibleRepository::delete);
    analysisProjectRepository.deleteAll();
    creditTransactionRepository.deleteAll();
    detectionTrackingJpaRepository.deleteAll();
  }

  private void grantJoeDoe(long credits) {
    creditTransactionRepository.save(
        CreditTransaction.builder()
            .id(randomUUID().toString())
            .userId(JOE_DOE_ID)
            .type(SUBSCRIPTION_GRANT)
            .movementType(CREDIT)
            .credits(credits)
            .creationDatetime(now())
            .build());
  }

  @SneakyThrows
  @Test
  void unknown_project_has_every_step_remaining_and_nothing_is_debited() {
    var projectKey = "v1:" + randomUUID();

    var actual =
        new AnalysisProjectApi(anApiClient()).getAnalysisProject(JOE_DOE_ID, projectKey, null);

    assertEquals(projectKey, actual.getProjectKey());
    assertFalse(actual.getAnalysis2dDone());
    assertFalse(actual.getModel3dDone());
    assertFalse(actual.getCreditConsumed());
    assertEquals(
        List.of(AnalysisProjectStep.ANALYSIS_2D, AnalysisProjectStep.MODEL_3D),
        actual.getRemainingSteps());
    assertEquals(1L, actual.getCreditCost());
    assertTrue(actual.getCreditsSufficient());
    assertEquals(0, consumptionsOfJoeDoe().size());
  }

  @SneakyThrows
  @Test
  void analysis_2d_then_model_3d_debits_a_single_credit() {
    var projectKey = "v1:" + randomUUID();
    var api = new AnalysisProjectApi(anApiClient());

    register2d(projectKey, null);
    var afterModel3d =
        api.registerAnalysisProjectModel3d(JOE_DOE_ID, projectKey, new RegisterModel3d());

    assertTrue(afterModel3d.getAnalysis2dDone());
    assertTrue(afterModel3d.getModel3dDone());
    assertTrue(afterModel3d.getCreditConsumed());
    assertEquals(List.of(), afterModel3d.getRemainingSteps());
    assertEquals(0L, afterModel3d.getCreditCost());
    assertEquals(1, consumptionsOfJoeDoe().size());
    assertEquals(99L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void model_3d_then_analysis_2d_debits_a_single_credit() {
    var projectKey = "v1:" + randomUUID();
    var api = new AnalysisProjectApi(anApiClient());

    var afterModel3d =
        api.registerAnalysisProjectModel3d(JOE_DOE_ID, projectKey, new RegisterModel3d());
    assertEquals(List.of(AnalysisProjectStep.ANALYSIS_2D), afterModel3d.getRemainingSteps());
    assertTrue(afterModel3d.getCreditConsumed());
    register2d(projectKey, null);

    var actual = api.getAnalysisProject(JOE_DOE_ID, projectKey, null);
    assertTrue(actual.getAnalysis2dDone());
    assertTrue(actual.getModel3dDone());
    assertEquals(1, consumptionsOfJoeDoe().size());
    assertEquals(99L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void registering_the_same_step_twice_debits_once() {
    var projectKey = "v1:" + randomUUID();
    var api = new AnalysisProjectApi(anApiClient());

    api.registerAnalysisProjectModel3d(JOE_DOE_ID, projectKey, new RegisterModel3d());
    api.registerAnalysisProjectModel3d(JOE_DOE_ID, projectKey, new RegisterModel3d());

    assertEquals(1, consumptionsOfJoeDoe().size());
    assertEquals(99L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void distinct_projects_are_debited_separately() {
    var api = new AnalysisProjectApi(anApiClient());

    api.registerAnalysisProjectModel3d(JOE_DOE_ID, "v1:" + randomUUID(), new RegisterModel3d());
    api.registerAnalysisProjectModel3d(JOE_DOE_ID, "v1:" + randomUUID(), new RegisterModel3d());

    assertEquals(2, consumptionsOfJoeDoe().size());
    assertEquals(98L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void external_reference_resolves_the_same_project_whatever_the_key() {
    var externalReference = "client-" + randomUUID();
    var api = new AnalysisProjectApi(anApiClient());

    register2d("v1:" + randomUUID(), externalReference);
    var actual =
        api.registerAnalysisProjectModel3d(
            JOE_DOE_ID,
            "v1:" + randomUUID(),
            new RegisterModel3d().externalReference(externalReference));

    assertTrue(actual.getAnalysis2dDone());
    assertTrue(actual.getModel3dDone());
    assertEquals(externalReference, actual.getExternalReference());
    assertEquals(1, consumptionsOfJoeDoe().size());
    var byReference = api.getAnalysisProject(JOE_DOE_ID, null, externalReference);
    assertEquals(actual.getProjectKey(), byReference.getProjectKey());
  }

  @SneakyThrows
  @Test
  void model_3d_without_enough_credits_is_refused_and_not_registered() {
    clearJoeDoeCreditContext();
    var projectKey = "v1:" + randomUUID();
    var api = new AnalysisProjectApi(anApiClient());

    var status = api.getAnalysisProject(JOE_DOE_ID, projectKey, null);
    assertFalse(status.getCreditsSufficient());
    var exception =
        assertThrows(
            ApiException.class,
            () ->
                api.registerAnalysisProjectModel3d(JOE_DOE_ID, projectKey, new RegisterModel3d()));

    assertEquals(402, exception.getCode());
    assertTrue(analysisProjectRepository.findAll().isEmpty());
    assertEquals(0, consumptionsOfJoeDoe().size());
  }

  @SneakyThrows
  @Test
  void project_status_requires_a_key_or_a_reference() {
    var api = new AnalysisProjectApi(anApiClient());

    var exception =
        assertThrows(ApiException.class, () -> api.getAnalysisProject(JOE_DOE_ID, null, null));

    assertEquals(400, exception.getCode());
  }

  @SneakyThrows
  private void register2d(String projectKey, String externalReference) {
    register2d(projectKey, externalReference, randomUUID().toString());
  }

  @SneakyThrows
  private void register2d(String projectKey, String externalReference, String detectionIdentifier) {
    new AnalysisProjectApi(anApiClient())
        .registerAnalysisProjectAnalysis2d(
            JOE_DOE_ID,
            projectKey,
            new RegisterAnalysis2d()
                .externalReference(externalReference)
                .detectionIdentifier(detectionIdentifier)
                .zone("zone-" + randomUUID())
                .address("dummyAddress")
                .initiator(
                    new DetectionInitiator()
                        .name("dummyInitiator")
                        .email("dummy@email.com")
                        .phoneNumber("0612345678"))
                .creationDatetime(now()));
  }

  @SneakyThrows
  @Test
  void registering_the_same_analysis_2d_twice_debits_once() {
    var projectKey = "v1:" + randomUUID();
    var detectionIdentifier = randomUUID().toString();

    register2d(projectKey, null, detectionIdentifier);
    register2d(projectKey, null, detectionIdentifier);

    assertEquals(1, consumptionsOfJoeDoe().size());
    assertEquals(1, detectionTrackingJpaRepository.count());
    assertEquals(99L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void deprecated_detection_tracking_endpoint_shares_the_project_debit() {
    var projectKey = "v1:" + randomUUID();
    new DetectionTrackingApi(anApiClient())
        .registerDetection(
            JOE_DOE_ID,
            List.of(
                new CreateDetectionTracking()
                    .detectionIdentifier(randomUUID().toString())
                    .projectKey(projectKey)
                    .zone("zone-" + randomUUID())
                    .address("dummyAddress")
                    .initiator(new DetectionInitiator().name("dummyInitiator"))
                    .creationDatetime(now())));

    var actual =
        new AnalysisProjectApi(anApiClient())
            .registerAnalysisProjectModel3d(JOE_DOE_ID, projectKey, new RegisterModel3d());

    assertTrue(actual.getAnalysis2dDone());
    assertTrue(actual.getModel3dDone());
    assertEquals(1, consumptionsOfJoeDoe().size());
  }

  private List<CreditTransaction> consumptionsOfJoeDoe() {
    return creditTransactionRepository.findAllByUserId(JOE_DOE_ID).stream()
        .filter(transaction -> CONSUMPTION.equals(transaction.getType()))
        .toList();
  }
}
