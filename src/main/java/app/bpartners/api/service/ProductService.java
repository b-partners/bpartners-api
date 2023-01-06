package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.model.Product;
import app.bpartners.api.repository.ProductRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static app.bpartners.api.service.utils.ProductUtils.getProductsFromFile;

@Service
@AllArgsConstructor
public class ProductService {
  private final ProductRepository repository;

  public List<Product> getProductsByAccount(String accountId, int page, int pageSize) {
    return repository.findAllByIdAccount(accountId, page, pageSize);
  }

  public List<Product> createProducts(String accountId, List<Product> toCreate) {
    return repository.saveAll(accountId, toCreate);
  }

  public List<CreateProduct> getDataFromFile(byte[] file) {
    return getProductsFromFile(new ByteArrayInputStream(file));
  }
}
