package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HCalendarStoredCredential;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CalendarStoredCredentialJpaRep
    extends JpaRepository<HCalendarStoredCredential, String> {
  boolean existsByIdUser(String idUser);

  boolean existsByAccessTokenAndRefreshTokenAndExpirationTimeMilliseconds(
      String accessToken, String refreshToken, Long expirationTime);

  HCalendarStoredCredential findByIdUser(String idUser);

  void deleteByIdUser(String idUser);
}
