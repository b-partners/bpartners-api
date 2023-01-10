package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Product;
import app.bpartners.api.model.mapper.ProductMapper;
import app.bpartners.api.repository.ProductRepository;
import app.bpartners.api.repository.jpa.ProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HProduct;
import java.util.List;
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
    List<HProduct> entities = jpaRepository.saveAll(toCreate.stream()
        .map(product -> mapper.toEntity(accountId, product))
        .collect(Collectors.toUnmodifiableList()));
    return entities.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }
}
