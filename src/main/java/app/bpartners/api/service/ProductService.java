package app.bpartners.api.service;

import app.bpartners.api.model.Product;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.ProductRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProductService {
  private final ProductRepository repository;

  public List<Product> getProductsByAccount(String accountId, String description, Boolean unique) {
    if (description != null) {
      return repository.findByIdAccountAndDescription(accountId, description);
    }
    if (unique == null) {
      throw new BadRequestException("Query parameter `unique` is mandatory.");
    }
    return repository.findByIdAccount(accountId, unique);
  }

  public List<Product> createProducts(String accountId, List<Product> toCreate) {
    return repository.saveAll(accountId, toCreate);
  }
}
