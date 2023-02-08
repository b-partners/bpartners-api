package app.bpartners.api.repository;


import app.bpartners.api.model.Product;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository {
  List<Product> findAllByIdAccount(String idAccount, Integer page, Integer pageSize);

  List<Product> saveAll(String accountId, List<Product> toCreate);

  List<Product> createProducts(String accountId, List<Product> toCreate);
}

