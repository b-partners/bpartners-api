package app.bpartners.api.repository.google.generic;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

@NoRepositoryBean
public interface CredentialJpaRepository<T> extends JpaRepository<T, String> {
  List<T> findAllByIdUserOrderByCreationDatetimeDesc(String idUser);

  void deleteByIdUser(String idUser);

  boolean existsByIdUser(String idUser);

  boolean existsByAccessTokenAndRefreshTokenAndExpirationTimeMilliseconds(
      String accessToken, String refreshToken, Long expirationTime);
}
