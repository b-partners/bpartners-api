package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.SubscriptionCardDisplayBrand.VISA;
import static app.bpartners.api.endpoint.rest.model.SubscriptionMethodType.CARD;
import static app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration.TWELVE_MONTHS;
import static app.bpartners.api.endpoint.rest.security.model.Role.ADMIN_ROLE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.mapper.SubscriptionPlanRestMapper;
import app.bpartners.api.endpoint.rest.mapper.UserSubscriptionCommitmentRestMapper;
import app.bpartners.api.endpoint.rest.mapper.UserSubscriptionPaymentMethodRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateUserSubscriptionCommitment;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.model.SubscriptionCard;
import app.bpartners.api.endpoint.rest.model.UpdateUserSubscriptionCommitmentAutoRenewalStatus;
import app.bpartners.api.endpoint.rest.model.UserSubscriptionPaymentMethod;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionBillingType;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.service.subscription.StripePaymentMethodService;
import app.bpartners.api.service.subscription.StripeSetupService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.user.UserService;
import com.stripe.model.PaymentMethod;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserControllerTest {
  private static final String STRIPE_CUSTOMER_IDENTIFIER = randomUUID().toString();
  StripeSetupService stripeSetupServiceMock = mock();
  CognitoComponent cognitoComponentMock = mock();
  UserService userServiceMock = mock();
  SubscriptionService subscriptionServiceMock = mock();
  SubscriptionProductRepository subscriptionProductRepositoryMock = mock();
  SubscriptionPlanRestMapper subscriptionPlanRestMapper = new SubscriptionPlanRestMapper();
  UserSubscriptionCommitmentRestMapper userSubscriptionCommitmentRestMapper =
      new UserSubscriptionCommitmentRestMapper(
          subscriptionProductRepositoryMock, subscriptionPlanRestMapper);
  StripePaymentMethodService stripePaymentMethodServiceMock = mock();
  UserSubscriptionPaymentMethodRestMapper userSubscriptionPaymentMethodRestMapper =
      new UserSubscriptionPaymentMethodRestMapper();
  UserController subject =
      new UserController(
          mock(),
          cognitoComponentMock,
          userServiceMock,
          subscriptionServiceMock,
          mock(),
          mock(),
          mock(),
          stripeSetupServiceMock,
          userSubscriptionCommitmentRestMapper,
          stripePaymentMethodServiceMock,
          userSubscriptionPaymentMethodRestMapper);

  @BeforeEach
  void setUp() {
    var email = randomUUID() + "@example.com";
    var userMock = mock(User.class);
    when(userMock.getUserSubscriptionId()).thenReturn(STRIPE_CUSTOMER_IDENTIFIER);
    when(userMock.getRoles()).thenReturn(List.of(ADMIN_ROLE));
    when(cognitoComponentMock.getEmailByToken(any())).thenReturn(email);
    when(userServiceMock.getUserByEmail(email)).thenReturn(userMock);
  }

  @Test
  void initiate_payment_method_url() {
    var successUrl = "http://localhost/" + randomUUID();
    var failureUrl = "http://localhost/" + randomUUID();
    var redirectionUrl = "https://checkout.stripe.com/pay/c/" + randomUUID();
    var userIdentifier = randomUUID().toString();
    var httpServletRequestMock = mock(HttpServletRequest.class);
    when(httpServletRequestMock.getHeader("Authorization"))
        .thenReturn("Bearer " + randomUUID().toString().replace("-", ""));

    var redirectionStatusUrls =
        new RedirectionStatusUrls().successUrl(successUrl).failureUrl(failureUrl);
    when(stripeSetupServiceMock.setupCheckoutSession(
            STRIPE_CUSTOMER_IDENTIFIER, redirectionStatusUrls))
        .thenReturn(
            new Redirection()
                .redirectionStatusUrls(redirectionStatusUrls)
                .redirectionUrl(redirectionUrl));

    var actual =
        subject.initiatePaymentMethodInsertion(
            httpServletRequestMock, userIdentifier, redirectionStatusUrls);

    assertEquals(
        new Redirection()
            .redirectionUrl(redirectionUrl)
            .redirectionStatusUrls(redirectionStatusUrls),
        actual);
  }

  @Test
  void initiate_payment_method_replacement_url() {
    var httpServletRequestMock = authenticatedRequest();
    var redirectionUrl = "https://checkout.stripe.com/pay/c/" + randomUUID();
    var redirectionStatusUrls =
        new RedirectionStatusUrls()
            .successUrl("http://localhost/" + randomUUID())
            .failureUrl("http://localhost/" + randomUUID());
    when(stripeSetupServiceMock.setupReplacementCheckoutSession(
            STRIPE_CUSTOMER_IDENTIFIER, redirectionStatusUrls))
        .thenReturn(
            new Redirection()
                .redirectionStatusUrls(redirectionStatusUrls)
                .redirectionUrl(redirectionUrl));

    var actual =
        subject.initiatePaymentMethodReplacement(
            httpServletRequestMock, randomUUID().toString(), redirectionStatusUrls);

    assertEquals(
        new Redirection()
            .redirectionUrl(redirectionUrl)
            .redirectionStatusUrls(redirectionStatusUrls),
        actual);
  }

  @Test
  void get_user_payment_methods_ok() throws Exception {
    var httpServletRequestMock = authenticatedRequest();
    var visaCards = List.of(visaCard());
    when(stripePaymentMethodServiceMock.getCardPaymentMethods(STRIPE_CUSTOMER_IDENTIFIER, true))
        .thenReturn(visaCards);

    var actual =
        subject.getUserPaymentMethods(httpServletRequestMock, randomUUID().toString(), true);

    assertEquals(
        List.of(
            new UserSubscriptionPaymentMethod()
                .type(CARD)
                .card(
                    new SubscriptionCard()
                        .displayBrand(VISA)
                        .lastFourDigits("4242")
                        .expirationMonth(12L)
                        .expirationYear(2050L))),
        actual);
  }

  @Test
  void get_user_payment_methods_ok_when_no_default_payment_method() throws Exception {
    var httpServletRequestMock = authenticatedRequest();
    when(stripePaymentMethodServiceMock.getCardPaymentMethods(STRIPE_CUSTOMER_IDENTIFIER, true))
        .thenReturn(List.of());

    var actual =
        subject.getUserPaymentMethods(httpServletRequestMock, randomUUID().toString(), true);

    assertEquals(List.of(), actual);
  }

  @Test
  void get_all_user_payment_methods_ok() throws Exception {
    var httpServletRequestMock = authenticatedRequest();
    var visaCards = List.of(visaCard(), visaCard());
    when(stripePaymentMethodServiceMock.getCardPaymentMethods(STRIPE_CUSTOMER_IDENTIFIER, false))
        .thenReturn(visaCards);

    var actual =
        subject.getUserPaymentMethods(httpServletRequestMock, randomUUID().toString(), false);

    assertEquals(2, actual.size());
  }

  private static PaymentMethod visaCard() {
    var cardDetails = mock(PaymentMethod.Card.class);
    when(cardDetails.getDisplayBrand()).thenReturn("visa");
    when(cardDetails.getLast4()).thenReturn("4242");
    when(cardDetails.getExpMonth()).thenReturn(12L);
    when(cardDetails.getExpYear()).thenReturn(2050L);
    var paymentMethod = mock(PaymentMethod.class);
    when(paymentMethod.getCard()).thenReturn(cardDetails);
    return paymentMethod;
  }

  @Test
  void save_user_subscription_commitments_ok() {
    var httpServletRequestMock = authenticatedRequest();
    var planId = randomUUID().toString();
    var commitmentStart = Instant.parse("2026-01-01T00:00:00Z");
    var approvalDatetime = Instant.parse("2025-12-31T00:00:00Z");
    var subscriptionProduct =
        SubscriptionProduct.builder()
            .id(planId)
            .name("Essentiel")
            .description("desc")
            .features(List.of("f1"))
            .billingType(SubscriptionBillingType.COMMITMENT)
            .priceInCentsWithoutVat(4900L)
            .vatPercent(2000L)
            .mostChosen(true)
            .deprecated(false)
            .displayPosition(1)
            .build();
    when(subscriptionProductRepositoryMock.findById(planId))
        .thenReturn(Optional.of(subscriptionProduct));
    when(subscriptionServiceMock.saveUserSubscriptionCommitments(anyList()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var actual =
        subject.saveUserSubscriptionCommitments(
            httpServletRequestMock,
            randomUUID().toString(),
            List.of(
                new CreateUserSubscriptionCommitment()
                    .subscriptionPlanIdentifier(planId)
                    .duration(TWELVE_MONTHS)
                    .commitmentStart(commitmentStart)
                    .approvalDatetime(approvalDatetime)));

    assertEquals(1, actual.size());
    var actualCommitment = actual.getFirst();
    assertNotNull(actualCommitment.getId());
    assertEquals(
        subscriptionPlanRestMapper.toRestDescription(subscriptionProduct),
        actualCommitment.getSubscriptionPlan());
    assertEquals(TWELVE_MONTHS, actualCommitment.getDuration());
    assertEquals(approvalDatetime, actualCommitment.getApprovalDatetime());
    assertEquals(commitmentStart, actualCommitment.getCommitmentStart());
    assertEquals(
        commitmentStart.atZone(ZoneId.of("Europe/Paris")).plusMonths(12).toInstant(),
        actualCommitment.getCommitmentEnd());
  }

  @Test
  void save_user_subscription_commitments_ko_when_mandatory_fields_missing() {
    var httpServletRequestMock = authenticatedRequest();

    var error =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.saveUserSubscriptionCommitments(
                    httpServletRequestMock,
                    randomUUID().toString(),
                    List.of(new CreateUserSubscriptionCommitment())));

    assertEquals(
        "subscriptionPlanIdentifier is mandatory. "
            + "duration is mandatory. "
            + "commitmentStart is mandatory. "
            + "approvalDatetime is mandatory. ",
        error.getMessage());
    verifyNoInteractions(subscriptionServiceMock);
  }

  @Test
  void get_user_subscription_commitments_ok() {
    var httpServletRequestMock = authenticatedRequest();
    var planId = randomUUID().toString();
    var commitmentId = randomUUID().toString();
    var commitmentStart = Instant.parse("2026-01-01T00:00:00Z");
    var commitmentEnd = Instant.parse("2027-01-01T00:00:00Z");
    var approvalDatetime = Instant.parse("2025-12-31T00:00:00Z");
    var subscriptionProduct =
        SubscriptionProduct.builder()
            .id(planId)
            .name("Essentiel")
            .description("desc")
            .features(List.of("f1"))
            .billingType(SubscriptionBillingType.COMMITMENT)
            .priceInCentsWithoutVat(4900L)
            .vatPercent(2000L)
            .mostChosen(true)
            .deprecated(false)
            .displayPosition(1)
            .build();
    when(subscriptionProductRepositoryMock.findById(planId))
        .thenReturn(Optional.of(subscriptionProduct));
    when(subscriptionServiceMock.getUserSubscriptionCommitments(any()))
        .thenReturn(
            List.of(
                app.bpartners.api.model.UserSubscriptionCommitment.builder()
                    .id(commitmentId)
                    .userId(randomUUID().toString())
                    .subscriptionPlanIdentifier(planId)
                    .duration(TWELVE_MONTHS)
                    .approvalDatetime(approvalDatetime)
                    .commitmentStartDatetime(commitmentStart)
                    .commitmentEndDatetime(commitmentEnd)
                    .build()));

    var actual =
        subject.getUserSubscriptionCommitments(httpServletRequestMock, randomUUID().toString());

    assertEquals(
        List.of(
            new app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitment()
                .id(commitmentId)
                .subscriptionPlan(subscriptionPlanRestMapper.toRestDescription(subscriptionProduct))
                .duration(TWELVE_MONTHS)
                .approvalDatetime(approvalDatetime)
                .commitmentStart(commitmentStart)
                .commitmentEnd(commitmentEnd)),
        actual);
  }

  @Test
  void get_user_subscription_commitments_ok_when_empty() {
    var httpServletRequestMock = authenticatedRequest();
    when(subscriptionServiceMock.getUserSubscriptionCommitments(any())).thenReturn(List.of());

    var actual =
        subject.getUserSubscriptionCommitments(httpServletRequestMock, randomUUID().toString());

    assertEquals(List.of(), actual);
  }

  @Test
  void update_user_subscription_commitment_auto_renewal_status_ok() {
    var httpServletRequestMock = authenticatedRequest();
    var planId = randomUUID().toString();
    var commitmentId = randomUUID().toString();
    var subscriptionProduct = SubscriptionProduct.builder().id(planId).name("Essentiel").build();
    when(subscriptionProductRepositoryMock.findById(planId))
        .thenReturn(Optional.of(subscriptionProduct));
    when(subscriptionServiceMock.updateUserSubscriptionCommitmentAutoRenewalStatus(
            any(), eq(commitmentId), eq(ENABLED)))
        .thenReturn(
            app.bpartners.api.model.UserSubscriptionCommitment.builder()
                .id(commitmentId)
                .userId(randomUUID().toString())
                .subscriptionPlanIdentifier(planId)
                .duration(TWELVE_MONTHS)
                .autoRenewalStatusHistory(
                    List.of(
                        app.bpartners.api.model.UserSubscriptionCommitmentAutoRenewalStatusHistory
                            .builder()
                            .id(randomUUID().toString())
                            .userSubscriptionCommitmentId(commitmentId)
                            .autoRenewalStatus(ENABLED)
                            .creationDatetime(Instant.parse("2026-01-01T00:00:00Z"))
                            .build()))
                .build());

    var actual =
        subject.updateUserSubscriptionCommitmentAutoRenewalStatus(
            httpServletRequestMock,
            randomUUID().toString(),
            commitmentId,
            new UpdateUserSubscriptionCommitmentAutoRenewalStatus()
                .automaticRenewalStatus(ENABLED));

    assertEquals(commitmentId, actual.getId());
    assertEquals(ENABLED, actual.getAutomaticRenewalStatus());
  }

  private HttpServletRequest authenticatedRequest() {
    var httpServletRequestMock = mock(HttpServletRequest.class);
    when(httpServletRequestMock.getHeader("Authorization"))
        .thenReturn("Bearer " + randomUUID().toString().replace("-", ""));
    return httpServletRequestMock;
  }
}
