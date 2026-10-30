package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.model.subscription.SecondaryUserStatus.ACTIVE;
import static app.bpartners.api.model.subscription.SecondaryUserStatus.INVITED;
import static app.bpartners.api.model.subscription.SecondaryUserStatus.REMOVED;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.SecondaryUser;
import app.bpartners.api.model.subscription.SecondaryUserStatus;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SecondaryUserRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import javax.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class SecondaryUserService {
  private static final String INVITATION_MAIL_TEMPLATE = "secondary_user_invitation_mail";
  private static final String INVITATION_MAIL_SUBJECT =
      "[birdia] Invitation à rejoindre un abonnement";
  private static final List<SecondaryUserStatus> LIVE_STATUSES = List.of(INVITED, ACTIVE);

  private final SecondaryUserRepository secondaryUserRepository;
  private final UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository;
  private final UserRepository userRepository;
  private final PrimaryUserResolver primaryUserResolver;
  private final SesService sesService;
  private final TemplateResolverEngine templateResolverEngine;

  public List<SecondaryUser> findAllByPrimaryUserId(String primaryUserId) {
    return secondaryUserRepository.findAllByPrimaryUserIdAndStatusInOrderByCreationDatetimeAsc(
        primaryUserId, LIVE_STATUSES);
  }

  public List<SecondaryUser> findPendingInvitationsOf(String userId) {
    var user = userRepository.getById(userId);
    return secondaryUserRepository.findAllByInvitedEmailIgnoreCaseAndStatus(
        user.getEmail(), INVITED);
  }

  public boolean hasSecondaryUsers(String primaryUserId) {
    return secondaryUserRepository.existsByPrimaryUserIdAndStatusIn(primaryUserId, LIVE_STATUSES);
  }

  public void requireNoSecondaryUsers(String primaryUserId) {
    if (hasSecondaryUsers(primaryUserId)) {
      throw new BadRequestException(
          "The subscription plan cannot be changed while secondary users exist,"
              + " remove them first");
    }
  }

  @Transactional
  public SecondaryUser invite(String primaryUserId, String invitedEmail) {
    var email = normalize(invitedEmail);
    var primaryUser = userRepository.getById(primaryUserId);
    var maxSecondaryUsers = maxSecondaryUsersOf(primaryUserId);
    var liveSecondaryUsers =
        secondaryUserRepository.countByPrimaryUserIdAndStatusIn(primaryUserId, LIVE_STATUSES);
    if (liveSecondaryUsers >= maxSecondaryUsers) {
      throw new BadRequestException(
          "The current subscription plan allows at most "
              + maxSecondaryUsers
              + " secondary users and "
              + liveSecondaryUsers
              + " already exist");
    }
    if (primaryUserResolver.isSecondary(primaryUserId)) {
      throw new BadRequestException("A secondary user cannot invite secondary users");
    }
    if (primaryUser.getEmail() != null && primaryUser.getEmail().equalsIgnoreCase(email)) {
      throw new BadRequestException("A user cannot invite themselves");
    }
    if (secondaryUserRepository.existsByPrimaryUserIdAndInvitedEmailIgnoreCaseAndStatusIn(
        primaryUserId, email, LIVE_STATUSES)) {
      throw new BadRequestException("Email " + email + " is already invited");
    }
    userRepository.findByEmail(email).ifPresent(this::checkAttachable);

    var saved =
        secondaryUserRepository.save(
            SecondaryUser.builder()
                .id(randomUUID().toString())
                .primaryUserId(primaryUserId)
                .invitedEmail(email)
                .status(INVITED)
                .creationDatetime(now())
                .updateDatetime(now())
                .build());
    sendInvitationMail(primaryUser, email);
    return saved;
  }

  @Transactional
  public SecondaryUser acceptInvitation(String userId, String invitationId) {
    var user = userRepository.getById(userId);
    var invitation =
        secondaryUserRepository
            .findById(invitationId)
            .filter(found -> INVITED.equals(found.getStatus()))
            .filter(found -> found.getInvitedEmail().equalsIgnoreCase(user.getEmail()))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "SecondaryUserInvitation(id=" + invitationId + ") not found"));
    checkAttachable(user);
    maxSecondaryUsersOf(invitation.getPrimaryUserId());
    return secondaryUserRepository.save(
        invitation.toBuilder()
            .secondaryUserId(userId)
            .status(ACTIVE)
            .updateDatetime(now())
            .build());
  }

  @Transactional
  public void remove(String primaryUserId, String secondaryUserId) {
    var secondaryUser =
        secondaryUserRepository
            .findByIdAndPrimaryUserId(secondaryUserId, primaryUserId)
            .filter(found -> !REMOVED.equals(found.getStatus()))
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "SecondaryUser(id="
                            + secondaryUserId
                            + ") not found for User(id="
                            + primaryUserId
                            + ")"));
    secondaryUserRepository.save(
        secondaryUser.toBuilder().status(REMOVED).updateDatetime(now()).build());
  }

  private int maxSecondaryUsersOf(String primaryUserId) {
    var activeProduct = activeUserSubscriptionProduct(primaryUserId);
    if (activeProduct.isEmpty()
        || activeProduct.get().isTrial()
        || activeProduct.get().getSubscriptionEndDatetime() != null) {
      throw new BadRequestException(
          "An active paid and not cancelled subscription is required to manage secondary users");
    }
    var maxSecondaryUsers = activeProduct.get().getSubscriptionProduct().getMaxSecondaryUsers();
    if (maxSecondaryUsers == null || maxSecondaryUsers <= 0) {
      throw new BadRequestException("The current subscription plan does not allow secondary users");
    }
    return maxSecondaryUsers;
  }

  private Optional<UserSubscriptionProduct> activeUserSubscriptionProduct(String userId) {
    return userSubscriptionProductJpaRepository.findAllActiveByUserId(userId, now()).stream()
        .findFirst();
  }

  private void checkAttachable(User user) {
    var userId = user.getId();
    if (primaryUserResolver.isSecondary(userId)) {
      throw new BadRequestException(
          "User(email=" + user.getEmail() + ") is already a secondary user");
    }
    if (hasSecondaryUsers(userId)) {
      throw new BadRequestException(
          "User(email=" + user.getEmail() + ") already has secondary users");
    }
    if (!userSubscriptionProductJpaRepository.findAllNotEndedByUserId(userId, now()).isEmpty()) {
      throw new BadRequestException(
          "User(email=" + user.getEmail() + ") has its own subscription, cancel it first");
    }
  }

  private void sendInvitationMail(User primaryUser, String invitedEmail) {
    var context = new Context();
    context.setVariable("primaryUserName", primaryUser.getName());
    context.setVariable("invitedEmail", invitedEmail);
    var htmlBody = templateResolverEngine.parseTemplateResolver(INVITATION_MAIL_TEMPLATE, context);
    try {
      sesService.sendEmail(invitedEmail, null, INVITATION_MAIL_SUBJECT, htmlBody);
    } catch (MessagingException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, "Invitation email not sent: " + e.getMessage());
    }
  }

  private static String normalize(String email) {
    if (email == null || email.isBlank()) {
      throw new BadRequestException("email is mandatory");
    }
    return email.trim().toLowerCase();
  }
}
