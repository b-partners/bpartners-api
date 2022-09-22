package app.bpartners.api.repository;

import app.bpartners.api.model.Product;
import java.util.List;

public interface ProductRepository {
  List<Product> findByIdAccount(String idAccount, boolean unique);

  List<Product> findByIdAccountAndDescription(String idAccount, String description);
}
