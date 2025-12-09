package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.UserWhiteListed;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserWhiteListedJpaRepository extends JpaRepository<UserWhiteListed, String> {
  @Query(
      value =
          """
              SELECT uw.*
              FROM "user_whitelisted" uw
              JOIN account_holder ah ON ah.id_user = uw.user_id
              WHERE ah.id = :accountHolderId
          """,
      nativeQuery = true)
  Optional<UserWhiteListed> findByIdAccountHolder(@Param("accountHolderId") String accountHolderId);
}
