package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.google.generic.CredentialJpaRepository;
import app.bpartners.api.repository.jpa.model.HSheetStoredCredential;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface SheetStoredCredentialJpaRep
    extends CredentialJpaRepository<HSheetStoredCredential> {
  boolean existsByIdUser(String idUser);

  boolean existsByAccessTokenAndRefreshTokenAndExpirationTimeMilliseconds(
      String accessToken, String refreshToken, Long expirationTime);

  List<HSheetStoredCredential> findAllByIdUserOrderByCreationDatetimeDesc(String idUser);

  void deleteByIdUser(String idUser);
}
