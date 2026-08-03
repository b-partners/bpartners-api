package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAccount;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountJpaRepository extends JpaRepository<HAccount, String> {
  List<HAccount> findByUser_Id(String userId);

  void deleteHAccountByUserId(String userId);
}
