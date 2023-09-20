package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.AccessToken;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AccessTokenMapper;
import app.bpartners.api.repository.SheetStoredCredentialRepository;
import app.bpartners.api.repository.jpa.SheetStoredCredentialJpaRep;
import app.bpartners.api.repository.jpa.model.HSheetStoredCredential;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class SheetStoredCredentialRepositoryImpl implements SheetStoredCredentialRepository {
  private final SheetStoredCredentialJpaRep jpaRep;
  private final AccessTokenMapper tokenMapper;

  @Override
  public AccessToken findLatestByIdUser(String idUser) {
    List<HSheetStoredCredential> credentials =
        jpaRep.findAllByIdUserOrderByCreationDatetimeDesc(idUser);
    if (credentials.isEmpty()) {
      throw new NotFoundException(
          "User(id=" + idUser + ") does not have google sheets access token");
    }
    return tokenMapper.toDomain(credentials.get(0));
  }
}
