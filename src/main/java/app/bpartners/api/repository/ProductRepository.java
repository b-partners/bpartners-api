package app.bpartners.api.repository;


import app.bpartners.api.endpoint.rest.model.OrderDirection;
import app.bpartners.api.model.Product;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository {
  List<Product> findAllByIdAccount(
      String idAccount, Integer page, Integer pageSize,
      OrderDirection descriptionOrder, OrderDirection unitPriceOrder, OrderDirection createdAtOrder
  );

  List<Product> findAllByIdAccount(String idAccount);

  List<Product> saveAll(String accountId, List<Product> toCreate);

}

