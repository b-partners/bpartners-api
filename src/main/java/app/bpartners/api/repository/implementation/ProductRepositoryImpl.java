package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.OrderDirection;
import app.bpartners.api.model.Product;
import app.bpartners.api.model.mapper.ProductMapper;
import app.bpartners.api.repository.ProductRepository;
import app.bpartners.api.repository.jpa.ProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HProduct;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
  private final ProductJpaRepository jpaRepository;
  private final ProductMapper mapper;

  @Override
  public List<Product> findAllByIdAccount(
      String idAccount, Integer page, Integer pageSize,
      OrderDirection descriptionOrder, OrderDirection unitPriceOrder, OrderDirection createdAtOrder
  ) {
    List<Sort.Order> orders = new ArrayList<>();
    if (descriptionOrder != null) {
      Sort.Order actual = new Sort.Order(Sort.Direction.valueOf(
          String.valueOf(descriptionOrder)), "description"
      );
      orders.add(actual);
    }
    if (unitPriceOrder != null) {
      Sort.Order actual = new Sort.Order(Sort.Direction.valueOf(
          String.valueOf(unitPriceOrder)), "unitPrice"
      );
      orders.add(actual);
    }
    if (createdAtOrder != null) {
      Sort.Order actual = new Sort.Order(Sort.Direction.valueOf(
          String.valueOf(createdAtOrder)), "createdAt"
      );
      orders.add(actual);
    }
    Pageable pageRequest = PageRequest.of(page, pageSize, Sort.by(orders));
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
