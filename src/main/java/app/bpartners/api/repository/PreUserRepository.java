package app.bpartners.api.repository;


import app.bpartners.api.model.PreUser;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface PreUserRepository {

  List<PreUser> saveAll(List<PreUser> toCreate);
}

