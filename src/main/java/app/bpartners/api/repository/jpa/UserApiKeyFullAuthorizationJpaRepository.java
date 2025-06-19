package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.UserApiKeyFullAuthorization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserApiKeyFullAuthorizationJpaRepository
    extends JpaRepository<UserApiKeyFullAuthorization, String> {
  Optional<UserApiKeyFullAuthorization> findByIdUser(String idUser);
}
