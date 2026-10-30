package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.subscription.SecondaryUser;
import app.bpartners.api.model.subscription.SecondaryUserStatus;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SecondaryUserRepository extends JpaRepository<SecondaryUser, String> {
  List<SecondaryUser> findAllByPrimaryUserIdAndStatusInOrderByCreationDatetimeAsc(
      String primaryUserId, Collection<SecondaryUserStatus> statuses);

  long countByPrimaryUserIdAndStatusIn(
      String primaryUserId, Collection<SecondaryUserStatus> statuses);

  Optional<SecondaryUser> findFirstBySecondaryUserIdAndStatus(
      String secondaryUserId, SecondaryUserStatus status);

  Optional<SecondaryUser> findByIdAndPrimaryUserId(String id, String primaryUserId);

  List<SecondaryUser> findAllByInvitedEmailIgnoreCaseAndStatus(
      String invitedEmail, SecondaryUserStatus status);

  boolean existsByPrimaryUserIdAndInvitedEmailIgnoreCaseAndStatusIn(
      String primaryUserId, String invitedEmail, Collection<SecondaryUserStatus> statuses);

  boolean existsByPrimaryUserIdAndStatusIn(
      String primaryUserId, Collection<SecondaryUserStatus> statuses);
}
