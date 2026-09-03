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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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
import app.bpartners.api.repository.DetectionTrackingRepository;
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
import org.springframework.data.domain.PageRequest;

class DetectionTrackingIT extends MockedThirdParties {
  private final String dummyApiKey = "dummyApiKey";
  @Autowired private DetectionTrackingService detectionTrackingService;
  @Autowired private DetectionTrackingRepository detectionTrackingRepository;
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
    var detectionIdentifier = randomUUID().toString();

    var actual =
        api.registerDetection(
            restJoeDoeUser().getId(), someCreateDetectionTracking(now, detectionIdentifier));

    var actualDomain =
        detectionTrackingService.findAllByIdUserBetween(restJoeDoeUser().getId(), now, now);
    var expectedRest = getExpectedRest(actual, now, detectionIdentifier);
    var expectedDomain = getExpectedDomain(actual, now, detectionIdentifier);
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
    var joeDoeId = restJoeDoeUser().getId();

    assertThrowsApiException(
        "{\"type\":\"402 PAYMENT_REQUIRED\",\"message\":\"Insufficient credits,"
            + " 1 required but 0 available\"}",
        () -> api.registerDetection(joeDoeId, payload));

    assertTrue(detectionTrackingService.findAllByIdUserBetween(joeDoeId, now, now).isEmpty());
    assertTrue(creditTransactionRepository.findAllByUserId(JOE_DOE_ID).isEmpty());
  }

  @SneakyThrows
  @Test
  void detection_already_registered_is_neither_saved_nor_billed_again() {
    var api = new DetectionTrackingApi(anApiClient());
    final var now = now().truncatedTo(ChronoUnit.MILLIS);
    var joeDoeId = restJoeDoeUser().getId();
    var detectionIdentifier = randomUUID().toString();
    var payload = someCreateDetectionTracking(now, detectionIdentifier);
    api.registerDetection(joeDoeId, payload);

    var actual = api.registerDetection(joeDoeId, payload);

    assertTrue(actual.isEmpty());
    assertEquals(1, detectionTrackingService.findAllByIdUserBetween(joeDoeId, now, now).size());
    verify(subscriptionService, times(1)).addConsumption(any());
    assertEquals(1, consumptionsOfJoeDoe().size());
    assertEquals(99L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void detection_without_identifier_is_always_registered() {
    var api = new DetectionTrackingApi(anApiClient());
    final var now = now().truncatedTo(ChronoUnit.MILLIS);
    var joeDoeId = restJoeDoeUser().getId();
    var payload = someCreateDetectionTracking(now, null);
    api.registerDetection(joeDoeId, payload);

    var actual = api.registerDetection(joeDoeId, payload);

    assertEquals(1, actual.size());
    assertEquals(2, detectionTrackingService.findAllByIdUserBetween(joeDoeId, now, now).size());
    assertEquals(2, consumptionsOfJoeDoe().size());
    assertEquals(98L, creditService.getCreditBalance(JOE_DOE_ID).getSpendableCredits());
  }

  @SneakyThrows
  @Test
  void retrieve_detection_tracking_list_by_user_id_is_ordered_by_creation_datetime_desc() {
    var api = new DetectionTrackingApi(anApiClient());
    var joeDoeId = restJoeDoeUser().getId();
    final var older = now().minusSeconds(60).truncatedTo(ChronoUnit.MILLIS);
    final var newer = now().truncatedTo(ChronoUnit.MILLIS);
    var olderId =
        api.registerDetection(joeDoeId, someCreateDetectionTracking(older)).getFirst().getId();
    var newerId =
        api.registerDetection(joeDoeId, someCreateDetectionTracking(newer)).getFirst().getId();

    var actualIds =
        api.retrieveDetectionTrackingListByUserId(joeDoeId, null, null, null).stream()
            .map(DetectionTracking::getId)
            .toList();

    assertTrue(actualIds.indexOf(newerId) < actualIds.indexOf(olderId));
  }

  @SneakyThrows
  @Test
  void retrieve_detection_tracking_list_is_paginated() {
    var api = new DetectionTrackingApi(anApiClient());
    var joeDoeId = restJoeDoeUser().getId();
    final var now = now().truncatedTo(ChronoUnit.MILLIS);
    api.registerDetection(joeDoeId, someCreateDetectionTracking(now.minusSeconds(1)));
    api.registerDetection(joeDoeId, someCreateDetectionTracking(now));

    var firstPage = api.retrieveDetectionTrackingListByUserId(joeDoeId, null, 1, 1);
    var secondPage = api.retrieveDetectionTrackingListByUserId(joeDoeId, null, 2, 1);

    assertEquals(1, firstPage.size());
    assertEquals(1, secondPage.size());
    assertNotEquals(firstPage.getFirst().getId(), secondPage.getFirst().getId());
  }

  @SneakyThrows
  @Test
  void retrieve_detection_tracking_list_filtered_by_search() {
    var api = new DetectionTrackingApi(anApiClient());
    var joeDoeId = restJoeDoeUser().getId();
    final var now = now().truncatedTo(ChronoUnit.MILLIS);
    var searchableZone = "searchableZone-" + randomUUID();
    var payload = someCreateDetectionTracking(now).getFirst().zone(searchableZone);
    var expectedId = api.registerDetection(joeDoeId, List.of(payload)).getFirst().getId();

    var matching =
        api.retrieveDetectionTrackingListByUserId(joeDoeId, "searchablezone-", null, null);
    var notMatching =
        api.retrieveDetectionTrackingListByUserId(joeDoeId, "noMatch-" + randomUUID(), null, null);

    assertEquals(List.of(expectedId), matching.stream().map(DetectionTracking::getId).toList());
    assertTrue(notMatching.isEmpty());
  }

  @SneakyThrows
  @Test
  void repository_find_all_by_id_user_and_search_accepts_null_search() {
    var api = new DetectionTrackingApi(anApiClient());
    var joeDoeId = restJoeDoeUser().getId();
    final var now = now().truncatedTo(ChronoUnit.MILLIS);
    var expectedId =
        api.registerDetection(joeDoeId, someCreateDetectionTracking(now)).getFirst().getId();

    var actual = detectionTrackingRepository.findAllByIdUser(joeDoeId, null, PageRequest.of(0, 10));

    assertTrue(
        actual.stream()
            .map(app.bpartners.api.model.detection.DetectionTracking::id)
            .toList()
            .contains(expectedId));
  }

  @SneakyThrows
  @Test
  void repository_find_all_by_id_user_and_search_filters_across_all_searchable_fields() {
    var api = new DetectionTrackingApi(anApiClient());
    var joeDoeId = restJoeDoeUser().getId();
    final var now = now().truncatedTo(ChronoUnit.MILLIS);
    var searchToken = randomUUID().toString();
    var zonePayload = someCreateDetectionTracking(now).getFirst().zone("zone-" + searchToken);
    var addressPayload =
        someCreateDetectionTracking(now).getFirst().address("address-" + searchToken);
    var initiatorPayload =
        someCreateDetectionTracking(now)
            .getFirst()
            .initiator(new DetectionInitiator().name("initiator-" + searchToken));
    var zoneId = api.registerDetection(joeDoeId, List.of(zonePayload)).getFirst().getId();
    var addressId = api.registerDetection(joeDoeId, List.of(addressPayload)).getFirst().getId();
    var initiatorId = api.registerDetection(joeDoeId, List.of(initiatorPayload)).getFirst().getId();

    var actualIds =
        detectionTrackingRepository
            .findAllByIdUser(joeDoeId, searchToken, PageRequest.of(0, 10))
            .stream()
            .map(app.bpartners.api.model.detection.DetectionTracking::id)
            .toList();

    assertEquals(3, actualIds.size());
    assertTrue(actualIds.containsAll(List.of(zoneId, addressId, initiatorId)));
  }

  private List<CreditTransaction> consumptionsOfJoeDoe() {
    return creditTransactionRepository.findAllByUserId(JOE_DOE_ID).stream()
        .filter(transaction -> CONSUMPTION.equals(transaction.getType()))
        .toList();
  }

  private List<CreateDetectionTracking> someCreateDetectionTracking(Instant creationDatetime) {
    return someCreateDetectionTracking(creationDatetime, randomUUID().toString());
  }

  private List<CreateDetectionTracking> someCreateDetectionTracking(
      Instant creationDatetime, String detectionIdentifier) {
    return List.of(
        new CreateDetectionTracking()
            .detectionIdentifier(detectionIdentifier)
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
      List<DetectionTracking> actual, Instant now, String detectionIdentifier) {
    return List.of(
        new app.bpartners.api.model.detection.DetectionTracking(
            actual.getFirst().getId(),
            "dummyZone",
            "dummyAddress",
            now,
            new app.bpartners.api.model.detection.DetectionInitiator(
                "dummyInitiator", "dummy@email.com", "0612345678"),
            userMock,
            detectionIdentifier));
  }

  private @NotNull List<DetectionTracking> getExpectedRest(
      List<DetectionTracking> actual, Instant now, String detectionIdentifier) {
    return List.of(
        new DetectionTracking()
            .id(actual.getFirst().getId())
            .detectionIdentifier(detectionIdentifier)
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
