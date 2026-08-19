package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.credit.CreditPack;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreditPackRepository extends JpaRepository<CreditPack, String> {
  List<CreditPack> findAllByDeprecatedFalseOrderByDisplayPosition(Pageable pageable);
}
