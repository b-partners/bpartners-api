package app.bpartners.api.repository;


import app.bpartners.api.endpoint.rest.model.OrderDirection;
import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.endpoint.rest.model.UpdateProductStatus;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Product;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository {
  List<Product> saveAll(String idUser, List<Product> toCreate);

  List<Product> findByIdUserAndCriteria(
      String idUser,
      ProductStatus status,
      Integer page,
      Integer pageSize,
      OrderDirection descriptionOrder,
      OrderDirection unitPriceOrder,
      OrderDirection createdAtOrder,
      String description,
      Fraction unitPrice);

  List<Product> updateStatuses(List<UpdateProductStatus> toUpdate);

  Product findById(String id);
}

