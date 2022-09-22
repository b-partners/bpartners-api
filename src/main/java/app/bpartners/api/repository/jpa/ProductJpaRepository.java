package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HProduct;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductJpaRepository extends JpaRepository<HProduct, String> {
  List<HProduct> findAllByIdAccount(String idAccount);

  @Query(value = "select p.* from \"product\" p where description = ?1 "
      + "order by created_datetime desc limit 1", nativeQuery = true)
  HProduct findDistinctByCriteriaOrderByDate(String description);
}
