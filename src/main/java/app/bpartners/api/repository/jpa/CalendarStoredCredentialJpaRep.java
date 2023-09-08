package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.google.generic.CredentialJpaRepository;
import app.bpartners.api.repository.jpa.model.HCalendarStoredCredential;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface CalendarStoredCredentialJpaRep
    extends CredentialJpaRepository<HCalendarStoredCredential> {
  boolean existsByIdUser(String idUser);

  boolean existsByAccessTokenAndRefreshTokenAndExpirationTimeMilliseconds(
      String accessToken, String refreshToken, Long expirationTime);

  List<HCalendarStoredCredential> findAllByIdUserOrderByCreationDatetimeDesc(String idUser);

  void deleteByIdUser(String idUser);
}
