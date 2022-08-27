package app.bpartners.api.repository;


import app.bpartners.api.model.PreUser;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface PreUserRepository {

  List<PreUser> createPreUsers(List<PreUser> toCreate);

  //TODO: will NEVER be used, remove: just provide read acces to DB as it's just for internal usage
  List<PreUser> getPreUsers(Pageable pageable);

  List<PreUser> getByCriteria(
      Pageable pageable, String firstName,
      String lastName, String email, String society, String phone);
}
