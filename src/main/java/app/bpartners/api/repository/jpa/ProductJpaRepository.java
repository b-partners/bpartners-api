package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.repository.jpa.model.HProduct;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductJpaRepository extends JpaRepository<HProduct, String> {
  Optional<HProduct> findByIdUserAndDescription(String idUser, String description);

  //TODO: replace this with criteria builder
  List<HProduct> findAllByIdUserAndStatusAndUnitPriceAndDescriptionContainingIgnoreCase(
      String idUser, ProductStatus status, String unitPrice, String description,
      Pageable pageable);

  List<HProduct> findAllByIdUserAndStatusAndDescriptionContainingIgnoreCase(
      String idUser, ProductStatus status, String description, Pageable pageable
  );

  List<HProduct> findByIdUserOrderByDescriptionAsc(String idUser);
}
