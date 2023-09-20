package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.AccessToken;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AccessTokenMapper;
import app.bpartners.api.repository.CalendarStoredCredentialRepository;
import app.bpartners.api.repository.jpa.CalendarStoredCredentialJpaRep;
import app.bpartners.api.repository.jpa.model.HCalendarStoredCredential;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class CalendarStoredCredentialRepositoryImpl implements CalendarStoredCredentialRepository {
  private final CalendarStoredCredentialJpaRep jpaRep;
  private final AccessTokenMapper tokenMapper;

  @Override
  public AccessToken findLatestByIdUser(String idUser) {
    List<HCalendarStoredCredential> credentials =
        jpaRep.findAllByIdUserOrderByCreationDatetimeDesc(idUser);
    if (credentials.isEmpty()) {
      throw new NotFoundException(
          "User(id=" + idUser + ") does not have google sheets access token");
    }
    return tokenMapper.toDomain(credentials.get(0));
  }
}
