package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.DEBIT;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.api.DetectionTrackingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.model.CreateDetectionTracking;
import app.bpartners.api.endpoint.rest.model.DetectionInitiator;
import app.bpartners.api.endpoint.rest.model.DetectionTracking;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.User;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.credit.CreditService;
import app.bpartners.api.service.detection.DetectionTrackingService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

class DetectionTrackingIT extends MockedThirdParties {
  private final String dummyApiKey = "dummyApiKey";
  @Autowired private DetectionTrackingService detectionTrackingService;
  @Autowired private CreditTransactionRepository creditTransactionRepository;
  @Autowired private UserSubscriptionEligibleJpaRepository userSubscriptionEligibleRepository;
  @Autowired private CreditService creditService;
  @MockBean private UserRepository userRepository;
  final CustomDateFormatter customDateFormatter = new CustomDateFormatter();
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
    creditTransactionRepository.save(
        CreditTransaction.builder()
            .id(randomUUID().toString())
            .userId(JOE_DOE_ID)
            .type(SUBSCRIPTION_GRANT)
            .movementType(CREDIT)
            .credits(100L)
            .creationDatetime(now())
            .build());
  }

  @AfterEach
  void tearDown() {
    clearJoeDoeCreditContext();
  }

  private void clearJoeDoeCreditContext() {
    userSubscriptionEligibleRepository
        .findByUserId(JOE_DOE_ID)
        .ifPresent(userSubscriptionEligibleRepository::delete);
    creditTransactionRepository.deleteAll();
  }

  @SneakyThrows
  @Test
  void save_and_read_detection_tracking() {
    var joeDoeClient = anApiClient();
    var api = new DetectionTrackingApi(joeDoeClient);
    final var now = now().truncatedTo(ChronoUnit.MILLIS);

    var actual =
        api.registerDetection(
            restJoeDoeUser().getId(),
            List.of(
                new CreateDetectionTracking()
                    .zone("dummyZone")
                    .address("dummyAddress")
                    .initiator(
                        new DetectionInitiator()
                            .name("dummyInitiator")
                            .email("dummy@email.com")
                            .phoneNumber("0612345678"))
                    .creationDatetime(now)));

    var actualDomain =
        detectionTrackingService.findAllByIdUserBetween(restJoeDoeUser().getId(), now, now);
    var expectedRest = getExpectedRest(actual, now);
    var expectedDomain = getExpectedDomain(actual, now);
    var subscriptionLogCaptor = ArgumentCaptor.forClass(SubscriptionConsumptionLog.class);
    verify(subscriptionService).addConsumption(subscriptionLogCaptor.capture());
    var expectedSubscriptionLog = getExpectedSubscriptionLog(subscriptionLogCaptor.getValue(), now);
    assertEquals(expectedSubscriptionLog, subscriptionLogCaptor.getValue());
    assertEquals(expectedDomain, actualDomain);
    assertEquals(expectedRest, actual);

    var consumptions =
        creditTransactionRepository.findAllByUserId(JOE_DOE_ID).stream()
            .filter(transaction -> CONSUMPTION.equals(transaction.getType()))
            .toList();
    assertEquals(1, consumptions.size());
    assertEquals(DEBIT, consumptions.getFirst().getMovementType());
    assertEquals(1L, consumptions.getFirst().getCredits());
    assertEquals("Analyse toiture : dummyAddress", consumptions.getFirst().getLabel());
    assertEquals(99L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void no_credit_is_debited_while_free_trial_is_active() {
    when(userMock.isPaymentMethodExists()).thenReturn(true);
    userSubscriptionEligibleRepository.save(
        UserSubscriptionEligible.builder()
            .id(randomUUID().toString())
            .userId(JOE_DOE_ID)
            .trialPeriodDays(30)
            .eligibleFrom(LocalDate.now())
            .creationDatetime(now())
            .build());
    var api = new DetectionTrackingApi(anApiClient());
    final var now = now().truncatedTo(ChronoUnit.MILLIS);

    api.registerDetection(restJoeDoeUser().getId(), someCreateDetectionTracking(now));

    verify(subscriptionService).addConsumption(any());
    assertTrue(consumptionsOfJoeDoe().isEmpty());
    assertEquals(100L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void detection_registration_is_rolled_back_when_credits_are_insufficient() {
    creditTransactionRepository.deleteAll();
    var api = new DetectionTrackingApi(anApiClient());
    final var now = now().truncatedTo(ChronoUnit.MILLIS);
    var payload = someCreateDetectionTracking(now);

    assertThrowsApiException(
        "{\"type\":\"402 PAYMENT_REQUIRED\",\"message\":\"Insufficient credits,"
            + " 1 required but 0 available\"}",
        () -> api.registerDetection(restJoeDoeUser().getId(), payload));

    assertTrue(
        detectionTrackingService
            .findAllByIdUserBetween(restJoeDoeUser().getId(), now, now)
            .isEmpty());
    assertTrue(creditTransactionRepository.findAllByUserId(JOE_DOE_ID).isEmpty());
  }

  private List<CreditTransaction> consumptionsOfJoeDoe() {
    return creditTransactionRepository.findAllByUserId(JOE_DOE_ID).stream()
        .filter(transaction -> CONSUMPTION.equals(transaction.getType()))
        .toList();
  }

  private List<CreateDetectionTracking> someCreateDetectionTracking(Instant creationDatetime) {
    return List.of(
        new CreateDetectionTracking()
            .zone("dummyZone")
            .address("dummyAddress")
            .initiator(
                new DetectionInitiator()
                    .name("dummyInitiator")
                    .email("dummy@email.com")
                    .phoneNumber("0612345678"))
            .creationDatetime(creationDatetime));
  }

  private SubscriptionConsumptionLog getExpectedSubscriptionLog(
      SubscriptionConsumptionLog actualConsumptionLog, Instant now) {
    var comment =
        String.format(
            "Analyse de toiture effectuée par le client dummyInitiator"
                + " (email=dummy@email.com, tel=0612345678) sur la zone dummyZone"
                + " à l'adresse dummyAddress le %s",
            customDateFormatter.formatFrenchDatetime(now));
    return SubscriptionConsumptionLog.builder()
        .id(actualConsumptionLog.getId())
        .usageMetric(1L)
        .consumptionType(ROOF_ANALYSIS)
        .creationDatetime(actualConsumptionLog.getCreationDatetime())
        .userId(restJoeDoeUser().getId())
        .consumptionUnit(UNIT)
        .comment(comment)
        .build();
  }

  private @NotNull List<app.bpartners.api.model.detection.DetectionTracking> getExpectedDomain(
      List<DetectionTracking> actual, Instant now) {
    return List.of(
        new app.bpartners.api.model.detection.DetectionTracking(
            actual.getFirst().getId(),
            "dummyZone",
            "dummyAddress",
            now,
            new app.bpartners.api.model.detection.DetectionInitiator(
                "dummyInitiator", "dummy@email.com", "0612345678"),
            userMock));
  }

  private @NotNull List<DetectionTracking> getExpectedRest(
      List<DetectionTracking> actual, Instant now) {
    return List.of(
        new DetectionTracking()
            .id(actual.getFirst().getId())
            .zone("dummyZone")
            .address("dummyAddress")
            .initiator(
                new DetectionInitiator()
                    .name("dummyInitiator")
                    .email("dummy@email.com")
                    .phoneNumber("0612345678"))
            .creationDatetime(now));
  }
}
