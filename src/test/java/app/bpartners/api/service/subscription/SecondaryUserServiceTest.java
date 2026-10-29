package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SecondaryUserStatus.ACTIVE;
import static app.bpartners.api.model.subscription.SecondaryUserStatus.INVITED;
import static app.bpartners.api.model.subscription.SecondaryUserStatus.REMOVED;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SecondaryUser;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SecondaryUserRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SecondaryUserServiceTest {
  static final String PRIMARY_ID = "primary_id";
  static final String INVITED_EMAIL = "invited@example.com";

  SecondaryUserRepository secondaryUserRepository = mock();
  UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository = mock();
  UserRepository userRepository = mock();
  PrimaryUserResolver primaryUserResolver = new PrimaryUserResolver(secondaryUserRepository);
  SesService sesService = mock();
  TemplateResolverEngine templateResolverEngine = mock();

  SecondaryUserService subject =
      new SecondaryUserService(
          secondaryUserRepository,
          userSubscriptionProductJpaRepository,
          userRepository,
          primaryUserResolver,
          sesService,
          templateResolverEngine);

  @BeforeEach
  void setUp() {
    when(userRepository.getById(PRIMARY_ID))
        .thenReturn(User.builder().id(PRIMARY_ID).email("primary@example.com").build());
    when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
    when(secondaryUserRepository.save(any(SecondaryUser.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(templateResolverEngine.parseTemplateResolver(anyString(), any())).thenReturn("<html/>");
  }

  UserSubscriptionProduct activeProduct(
      Integer maxSecondaryUsers, BillingInterval interval, boolean cancelled) {
    return UserSubscriptionProduct.builder()
        .userId(PRIMARY_ID)
        .billingInterval(interval)
        .subscriptionEndDatetime(cancelled ? now().plus(10, DAYS) : null)
        .subscriptionProduct(
            SubscriptionProduct.builder().maxSecondaryUsers(maxSecondaryUsers).build())
        .build();
  }

  void givenActiveProduct(UserSubscriptionProduct product) {
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(anyString(), any()))
        .thenReturn(List.of(product));
  }

  @Test
  void invite_saves_invitation_and_sends_mail() throws Exception {
    givenActiveProduct(activeProduct(5, BillingInterval.MONTHLY, false));
    when(secondaryUserRepository.countByPrimaryUserIdAndStatusIn(anyString(), any()))
        .thenReturn(4L);

    var actual = subject.invite(PRIMARY_ID, "  Invited@Example.com ");

    assertEquals(INVITED, actual.getStatus());
    assertEquals(INVITED_EMAIL, actual.getInvitedEmail());
    assertEquals(PRIMARY_ID, actual.getPrimaryUserId());
    verify(sesService).sendEmail(anyString(), any(), anyString(), anyString());
  }

  @Test
  void invite_refused_when_plan_does_not_allow_secondary_users() {
    givenActiveProduct(activeProduct(null, BillingInterval.MONTHLY, false));

    assertThrows(BadRequestException.class, () -> subject.invite(PRIMARY_ID, INVITED_EMAIL));
    verify(secondaryUserRepository, never()).save(any());
  }

  @Test
  void invite_refused_when_limit_reached() {
    givenActiveProduct(activeProduct(5, BillingInterval.MONTHLY, false));
    when(secondaryUserRepository.countByPrimaryUserIdAndStatusIn(anyString(), any()))
        .thenReturn(5L);

    assertThrows(BadRequestException.class, () -> subject.invite(PRIMARY_ID, INVITED_EMAIL));
    verify(secondaryUserRepository, never()).save(any());
  }

  @Test
  void invite_refused_on_trial() {
    givenActiveProduct(activeProduct(5, null, true));

    assertThrows(BadRequestException.class, () -> subject.invite(PRIMARY_ID, INVITED_EMAIL));
  }

  @Test
  void invite_refused_when_subscription_cancelled() {
    givenActiveProduct(activeProduct(5, BillingInterval.MONTHLY, true));

    assertThrows(BadRequestException.class, () -> subject.invite(PRIMARY_ID, INVITED_EMAIL));
  }

  @Test
  void invite_refused_without_subscription() {
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(anyString(), any()))
        .thenReturn(List.of());

    assertThrows(BadRequestException.class, () -> subject.invite(PRIMARY_ID, INVITED_EMAIL));
  }

  @Test
  void invite_refused_when_invited_user_has_own_subscription() {
    givenActiveProduct(activeProduct(5, BillingInterval.MONTHLY, false));
    var invitedUser = User.builder().id("invited_id").email(INVITED_EMAIL).build();
    when(userRepository.findByEmail(INVITED_EMAIL)).thenReturn(Optional.of(invitedUser));
    when(userSubscriptionProductJpaRepository.findAllNotEndedByUserId(anyString(), any()))
        .thenReturn(List.of(activeProduct(1, BillingInterval.MONTHLY, false)));

    assertThrows(BadRequestException.class, () -> subject.invite(PRIMARY_ID, INVITED_EMAIL));
    verify(secondaryUserRepository, never()).save(any());
  }

  @Test
  void invite_refused_when_email_already_invited() {
    givenActiveProduct(activeProduct(5, BillingInterval.MONTHLY, false));
    when(secondaryUserRepository.existsByPrimaryUserIdAndInvitedEmailIgnoreCaseAndStatusIn(
            anyString(), anyString(), any()))
        .thenReturn(true);

    assertThrows(BadRequestException.class, () -> subject.invite(PRIMARY_ID, INVITED_EMAIL));
  }

  @Test
  void invite_refused_when_inviter_is_secondary() {
    givenActiveProduct(activeProduct(5, BillingInterval.MONTHLY, false));
    when(secondaryUserRepository.findFirstBySecondaryUserIdAndStatus(PRIMARY_ID, ACTIVE))
        .thenReturn(
            Optional.of(
                SecondaryUser.builder().primaryUserId("other_primary").status(ACTIVE).build()));

    assertThrows(BadRequestException.class, () -> subject.invite(PRIMARY_ID, INVITED_EMAIL));
  }

  @Test
  void accept_invitation_activates_secondary_user() {
    var invitation =
        SecondaryUser.builder()
            .id("invitation_id")
            .primaryUserId(PRIMARY_ID)
            .invitedEmail(INVITED_EMAIL)
            .status(INVITED)
            .build();
    when(userRepository.getById("invited_id"))
        .thenReturn(User.builder().id("invited_id").email("Invited@example.com").build());
    when(secondaryUserRepository.findById("invitation_id")).thenReturn(Optional.of(invitation));
    givenActiveProduct(activeProduct(5, BillingInterval.MONTHLY, false));

    var actual = subject.acceptInvitation("invited_id", "invitation_id");

    assertEquals(ACTIVE, actual.getStatus());
    assertEquals("invited_id", actual.getSecondaryUserId());
  }

  @Test
  void accept_invitation_refused_for_another_email() {
    var invitation =
        SecondaryUser.builder()
            .id("invitation_id")
            .primaryUserId(PRIMARY_ID)
            .invitedEmail(INVITED_EMAIL)
            .status(INVITED)
            .build();
    when(userRepository.getById("intruder_id"))
        .thenReturn(User.builder().id("intruder_id").email("intruder@example.com").build());
    when(secondaryUserRepository.findById("invitation_id")).thenReturn(Optional.of(invitation));

    assertThrows(
        NotFoundException.class, () -> subject.acceptInvitation("intruder_id", "invitation_id"));
  }

  @Test
  void remove_marks_secondary_user_as_removed() {
    var secondaryUser =
        SecondaryUser.builder().id("sec_id").primaryUserId(PRIMARY_ID).status(ACTIVE).build();
    when(secondaryUserRepository.findByIdAndPrimaryUserId("sec_id", PRIMARY_ID))
        .thenReturn(Optional.of(secondaryUser));

    subject.remove(PRIMARY_ID, "sec_id");

    var captor = ArgumentCaptor.forClass(SecondaryUser.class);
    verify(secondaryUserRepository).save(captor.capture());
    assertEquals(REMOVED, captor.getValue().getStatus());
  }

  @Test
  void remove_unknown_secondary_user_is_not_found() {
    when(secondaryUserRepository.findByIdAndPrimaryUserId(anyString(), anyString()))
        .thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> subject.remove(PRIMARY_ID, "unknown"));
  }

  @Test
  void plan_change_is_blocked_while_secondary_users_exist() {
    when(secondaryUserRepository.existsByPrimaryUserIdAndStatusIn(anyString(), any()))
        .thenReturn(true);

    assertThrows(BadRequestException.class, () -> subject.requireNoSecondaryUsers(PRIMARY_ID));
  }

  @Test
  void plan_change_is_allowed_without_secondary_users() {
    when(secondaryUserRepository.existsByPrimaryUserIdAndStatusIn(anyString(), any()))
        .thenReturn(false);

    subject.requireNoSecondaryUsers(PRIMARY_ID);
  }
}
