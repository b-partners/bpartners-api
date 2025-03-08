package app.bpartners.api.repository.jpa;

import static jakarta.persistence.LockModeType.PESSIMISTIC_WRITE;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.repository.jpa.model.HUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserJpaRepository extends JpaRepository<HUser, String> {

  Long countByStatus(EnableStatus status);

  Optional<HUser> findByApiKey(String apiKey);

  @Lock(PESSIMISTIC_WRITE)
  Optional<HUser> findByAccessToken(String token);

  Optional<HUser> findByEmail(String email);

  HUser getByEmail(String email);

  @Query("select u from HUser u join HAccount a" + " on u.id = a.user.id" + " where a.id = ?1")
  HUser getByAccountId(String accountId);

  @Lock(PESSIMISTIC_WRITE)
  @Query("select u from HUser u join HAccount a" + " on u.id = a.user.id" + " where a.id = ?1")
  HUser pwGetByAccountId(String accountId);

  @Lock(PESSIMISTIC_WRITE)
  HUser getHUserById(String id);

  HUser getHUserByGeoJobsApikey(String apiKey);

  @Query("select u from HUser u where u.userSubscriptionE2Id is null and u.status = 'ENABLED'")
  List<HUser> getEnabledUsersWithoutSubscription();

  @Query("select u from HUser u where u.userSubscriptionE2Id is not null")
  List<HUser> getUsersWithSubscription();

  @Query("select u from HUser u where u.parentUser.id = ?1")
  List<HUser> findSubordinatesUsersByParentId(String parentId);
}
