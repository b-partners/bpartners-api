package app.bpartners.api.repository;


import app.bpartners.api.model.PreUser;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface PreUserRepository {

  List<PreUser> createPreUsers(List<PreUser> toCreate);

  List<PreUser> getPreUsers(Pageable pageable);

  List<PreUser> getByCriteria(
      Pageable pageable, String firstName,
      String lastName, String email, String society, String phoneNumber);
}
