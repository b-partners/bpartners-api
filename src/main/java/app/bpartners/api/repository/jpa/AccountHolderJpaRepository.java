package app.bpartners.api.repository.jpa;


import app.bpartners.api.repository.jpa.model.HAccountHolder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountHolderJpaRepository extends JpaRepository<HAccountHolder, String> {
  HAccountHolder getHAccountHolderByAccountId(String accountId);
}
