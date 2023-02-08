package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HProduct;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<HProduct, String> {
  List<HProduct> findAllByIdAccount(String idAccount, Pageable pageable);

  List<HProduct> findAllByIdAccount(String idAccount);

}
