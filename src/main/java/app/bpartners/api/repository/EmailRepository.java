package app.bpartners.api.repository;

import app.bpartners.api.model.Email;
import java.util.List;

public interface EmailRepository {
  Email findById(String id);

  List<Email> findAllByUserId(String userId);

  List<Email> saveAll(List<Email> emails);
}
