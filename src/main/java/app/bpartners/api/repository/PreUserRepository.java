package app.bpartners.api.repository;


import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.User;
import app.bpartners.api.model.entity.HPreUser;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface PreUserRepository {

  public PreUser getPreUserById(String id);

  public List<PreUser> getPreUsers(Pageable pageable);

  List<PreUser> savePreUsers(List<HPreUser> hPreUsers);


}
