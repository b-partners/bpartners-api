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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.AnalysisProjectApi;
import app.bpartners.api.endpoint.rest.api.DetectionTrackingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AnalysisProjectPhase;
import app.bpartners.api.endpoint.rest.model.AnalysisProjectStep;
import app.bpartners.api.endpoint.rest.model.CreateDetectionTracking;
import app.bpartners.api.endpoint.rest.model.CreateDraftAnalysisProject;
import app.bpartners.api.endpoint.rest.model.DetectionInitiator;
import app.bpartners.api.endpoint.rest.model.Register2d;
import app.bpartners.api.endpoint.rest.model.Register3d;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.User;
import app.bpartners.api.model.activity.UserActivity;
import app.bpartners.api.model.activity.UserActivityType;
import app.bpartners.api.model.activity.UserActivityUnit;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.AnalysisProjectRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.DetectionTrackingJpaRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.activity.UserActivityService;
import app.bpartners.api.service.credit.CreditService;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
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
  @MockBean private UserActivityService userActivityService;
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
    when(userActivityService.record(any())).thenAnswer(invocation -> invocation.getArgument(0));

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
    var afterModel3d = api.registerAnalysisProject3d(JOE_DOE_ID, projectKey, new Register3d());

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
  void model_3d_records_a_single_user_activity() {
    var projectKey = "v1:" + randomUUID();
    var api = new AnalysisProjectApi(anApiClient());

    api.registerAnalysisProject3d(JOE_DOE_ID, projectKey, new Register3d().address("some address"));
    api.registerAnalysisProject3d(JOE_DOE_ID, projectKey, new Register3d().address("some address"));

    var captor = ArgumentCaptor.forClass(UserActivity.class);
    verify(userActivityService, times(1)).record(captor.capture());
    var actual = captor.getValue();
    assertEquals(JOE_DOE_ID, actual.getUserId());
    assertEquals(UserActivityType.ROOF_MODEL_3D, actual.getActivityType());
    assertEquals(UserActivityUnit.UNIT, actual.getActivityUnit());
    assertEquals(1L, actual.getQuantity());
    assertEquals("Modèle 3D : some address", actual.getDescription());
  }

  @SneakyThrows
  @Test
  void model_3d_then_analysis_2d_debits_a_single_credit() {
    var projectKey = "v1:" + randomUUID();
    var api = new AnalysisProjectApi(anApiClient());

    var afterModel3d = api.registerAnalysisProject3d(JOE_DOE_ID, projectKey, new Register3d());
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

    api.registerAnalysisProject3d(JOE_DOE_ID, projectKey, new Register3d());
    api.registerAnalysisProject3d(JOE_DOE_ID, projectKey, new Register3d());

    assertEquals(1, consumptionsOfJoeDoe().size());
    assertEquals(99L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void distinct_projects_are_debited_separately() {
    var api = new AnalysisProjectApi(anApiClient());

    api.registerAnalysisProject3d(JOE_DOE_ID, "v1:" + randomUUID(), new Register3d());
    api.registerAnalysisProject3d(JOE_DOE_ID, "v1:" + randomUUID(), new Register3d());

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
        api.registerAnalysisProject3d(
            JOE_DOE_ID,
            "v1:" + randomUUID(),
            new Register3d().externalReference(externalReference));

    assertTrue(actual.getAnalysis2dDone());
    assertTrue(actual.getModel3dDone());
    assertEquals(externalReference, actual.getExternalReference());
    assertEquals(1, consumptionsOfJoeDoe().size());
    var byReference =
        api.listAnalysisProjects(JOE_DOE_ID, null, externalReference, null, null, null, null);
    assertEquals(1, byReference.size());
    assertEquals(actual.getProjectKey(), byReference.getFirst().getProjectKey());
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
            () -> api.registerAnalysisProject3d(JOE_DOE_ID, projectKey, new Register3d()));

    assertEquals(402, exception.getCode());
    assertTrue(analysisProjectRepository.findAll().isEmpty());
    assertEquals(0, consumptionsOfJoeDoe().size());
    verify(userActivityService, never()).record(any());
  }

  @SneakyThrows
  @Test
  void list_exposes_the_2d_description_and_filters_by_search() {
    var api = new AnalysisProjectApi(anApiClient());
    var token = randomUUID().toString();
    var projectKey = "v1:" + randomUUID();
    api.registerAnalysisProject2d(
        JOE_DOE_ID,
        projectKey,
        new Register2d()
            .detectionIdentifier("detection-" + token)
            .zone("zone-" + token)
            .address("address-" + token)
            .initiator(
                new DetectionInitiator()
                    .name("initiator-" + token)
                    .email("dummy@email.com")
                    .phoneNumber("0612345678"))
            .creationDatetime(now()));
    api.registerAnalysisProject3d(
        JOE_DOE_ID,
        "v1:" + randomUUID(),
        new Register3d().address("other-address-" + randomUUID()));

    var all = api.listAnalysisProjects(JOE_DOE_ID, null, null, null, null, null, null);
    var filtered =
        api.listAnalysisProjects(JOE_DOE_ID, null, null, "ADDRESS-" + token, null, null, null);

    assertEquals(2, all.size());
    assertEquals(1, filtered.size());
    var actual = filtered.getFirst();
    assertEquals(projectKey, actual.getProjectKey());
    assertEquals("detection-" + token, actual.getDetectionIdentifier());
    assertEquals("zone-" + token, actual.getZone());
    assertEquals("address-" + token, actual.getAddress());
    assertEquals("initiator-" + token, actual.getInitiator().getName());
    assertEquals("dummy@email.com", actual.getInitiator().getEmail());
    assertEquals("0612345678", actual.getInitiator().getPhoneNumber());
    assertTrue(actual.getAnalysis2dDone());
    assertEquals(List.of(AnalysisProjectStep.MODEL_3D), actual.getRemainingSteps());
  }

  @SneakyThrows
  @Test
  void list_is_ordered_by_creation_datetime_desc_and_filtered_by_key() {
    var api = new AnalysisProjectApi(anApiClient());
    var firstKey = "v1:" + randomUUID();
    var secondKey = "v1:" + randomUUID();
    api.registerAnalysisProject3d(JOE_DOE_ID, firstKey, new Register3d());
    api.registerAnalysisProject3d(JOE_DOE_ID, secondKey, new Register3d());

    var all = api.listAnalysisProjects(JOE_DOE_ID, null, null, null, null, null, null);
    var byKey = api.listAnalysisProjects(JOE_DOE_ID, firstKey, null, null, null, null, null);

    assertEquals(List.of(secondKey, firstKey), all.stream().map(p -> p.getProjectKey()).toList());
    assertEquals(1, byKey.size());
    assertEquals(firstKey, byKey.getFirst().getProjectKey());
  }

  @SneakyThrows
  private void register2d(String projectKey, String externalReference) {
    register2d(projectKey, externalReference, randomUUID().toString());
  }

  @SneakyThrows
  private void register2d(String projectKey, String externalReference, String detectionIdentifier) {
    new AnalysisProjectApi(anApiClient())
        .registerAnalysisProject2d(
            JOE_DOE_ID,
            projectKey,
            new Register2d()
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
            .registerAnalysisProject3d(JOE_DOE_ID, projectKey, new Register3d());

    assertTrue(actual.getAnalysis2dDone());
    assertTrue(actual.getModel3dDone());
    assertEquals(1, consumptionsOfJoeDoe().size());
  }

  @SneakyThrows
  @Test
  void put_creates_a_draft_without_debiting_and_is_idempotent() {
    var projectKey = randomUUID().toString();
    var api = new AnalysisProjectApi(anApiClient());
    var body =
        new CreateDraftAnalysisProject()
            .address("draftAddress")
            .zone("{\"type\":\"Polygon\",\"coordinates\":[]}")
            .areaPictureId("areaPicture-" + projectKey)
            .prospectId("prospect-" + projectKey);

    var balanceBefore = creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits();
    var created = api.createDraftAnalysisProject(JOE_DOE_ID, projectKey, body);
    var again = api.createDraftAnalysisProject(JOE_DOE_ID, projectKey, body);

    assertEquals(AnalysisProjectPhase.DRAFT, created.getStatus());
    assertEquals(projectKey, created.getProjectKey());
    assertEquals("draftAddress", created.getAddress());
    assertEquals("areaPicture-" + projectKey, created.getAreaPictureId());
    assertEquals("prospect-" + projectKey, created.getProspectId());
    assertEquals(created.getProjectKey(), again.getProjectKey());
    assertEquals(created.getStatus(), again.getStatus());
    assertEquals(
        1, api.listAnalysisProjects(JOE_DOE_ID, projectKey, null, null, null, null, null).size());
    assertEquals(0, consumptionsOfJoeDoe().size());
    assertEquals(balanceBefore, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void draft_is_listed_and_follows_the_status_lifecycle() {
    var projectKey = randomUUID().toString();
    var api = new AnalysisProjectApi(anApiClient());
    api.createDraftAnalysisProject(
        JOE_DOE_ID, projectKey, new CreateDraftAnalysisProject().address("a").zone("z"));

    var drafts =
        api.listAnalysisProjects(
            JOE_DOE_ID, null, null, null, null, null, AnalysisProjectPhase.DRAFT);
    assertEquals(1, drafts.size());

    register2d(projectKey, null);
    var inProgress =
        api.listAnalysisProjects(
            JOE_DOE_ID, null, null, null, null, null, AnalysisProjectPhase.IN_PROGRESS);
    assertEquals(AnalysisProjectPhase.IN_PROGRESS, inProgress.get(0).getStatus());
    assertEquals(
        0,
        api.listAnalysisProjects(
                JOE_DOE_ID, null, null, null, null, null, AnalysisProjectPhase.DRAFT)
            .size());

    var done = api.registerAnalysisProject3d(JOE_DOE_ID, projectKey, new Register3d());
    assertEquals(AnalysisProjectPhase.DONE, done.getStatus());
    assertEquals(1, consumptionsOfJoeDoe().size());

    var afterDone =
        api.createDraftAnalysisProject(
            JOE_DOE_ID, projectKey, new CreateDraftAnalysisProject().address("b").zone("z2"));
    assertEquals(AnalysisProjectPhase.DONE, afterDone.getStatus());
    assertEquals(done.getAddress(), afterDone.getAddress());
  }

  @SneakyThrows
  @Test
  void registrations_on_the_key_of_a_draft_complete_it_instead_of_opening_another_project() {
    var sessionId = randomUUID().toString();
    var externalReference = "crm-" + randomUUID();
    var api = new AnalysisProjectApi(anApiClient());
    api.createDraftAnalysisProject(
        JOE_DOE_ID,
        sessionId,
        new CreateDraftAnalysisProject()
            .address("draftAddress")
            .zone("z")
            .externalReference(externalReference)
            .areaPictureId("areaPicture-" + sessionId)
            .prospectId("prospect-" + sessionId));

    register2d(sessionId, externalReference);
    var done =
        api.registerAnalysisProject3d(
            JOE_DOE_ID, sessionId, new Register3d().externalReference(externalReference));

    var projects = api.listAnalysisProjects(JOE_DOE_ID, null, null, null, null, null, null);
    assertEquals(1, projects.size());
    assertEquals(sessionId, projects.get(0).getProjectKey());
    assertEquals(AnalysisProjectPhase.DONE, done.getStatus());
    assertEquals("areaPicture-" + sessionId, done.getAreaPictureId());
    assertEquals("prospect-" + sessionId, done.getProspectId());
    assertEquals(externalReference, done.getExternalReference());
    assertTrue(done.getAnalysis2dDone());
    assertTrue(done.getModel3dDone());
    assertEquals(1, consumptionsOfJoeDoe().size());
    assertEquals(99L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  private List<CreditTransaction> consumptionsOfJoeDoe() {
    return creditTransactionRepository.findAllByUserId(JOE_DOE_ID).stream()
        .filter(transaction -> CONSUMPTION.equals(transaction.getType()))
        .toList();
  }
}
