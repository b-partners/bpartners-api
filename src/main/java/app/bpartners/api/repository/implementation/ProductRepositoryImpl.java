package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Product;
import app.bpartners.api.model.mapper.ProductMapper;
import app.bpartners.api.repository.ProductRepository;
import app.bpartners.api.repository.jpa.ProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HProduct;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
  private final ProductJpaRepository jpaRepository;
  private final ProductMapper mapper;

  @Override
  public List<Product> findAllByIdAccount(String idAccount, Integer page, Integer pageSize) {
    Pageable pageRequest = PageRequest.of(page, pageSize);
    return jpaRepository.findAllByIdAccount(idAccount, pageRequest).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }


  @Override
  public List<Product> saveAll(String accountId, List<Product> toCreate) {
    for (Product product : toCreate) {
      HProduct entity = mapper.toEntity(accountId, product);
      if (product.getId() != null) {
        Optional<HProduct> existingProduct = jpaRepository.findById(product.getId());
        if (existingProduct.isPresent()) {
          existingProduct.get().setDescription(entity.getDescription());
          existingProduct.get().setUnitPrice(entity.getUnitPrice());
          existingProduct.get().setIdAccount(accountId);
          existingProduct.get().setVatPercent(entity.getVatPercent());
          mapper.toDomain(jpaRepository.save(existingProduct.get()));
        }
      } else {
        mapper.toDomain(jpaRepository.save(entity));
      }
    }
    List<HProduct> products = jpaRepository.findAllByIdAccount(accountId);

    return products.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Product> createProducts(String accountId, List<Product> toCreate) {
    List<HProduct> entities = jpaRepository.saveAll(toCreate.stream()
        .map(product -> mapper.toEntity(accountId, product))
        .collect(Collectors.toUnmodifiableList()));
    return entities.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

}
