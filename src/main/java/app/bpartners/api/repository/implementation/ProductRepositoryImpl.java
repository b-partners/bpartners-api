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
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
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
    List<Order> orders = retrieveOrders(descriptionOrder, unitPriceOrder, createdAtOrder);
    Pageable pageRequest = PageRequest.of(page, pageSize, Sort.by(orders));
    return jpaRepository.findAllByIdAccount(idAccount, pageRequest).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  private List<Order> retrieveOrders(OrderDirection descriptionOrder,
                                     OrderDirection unitPriceOrder,
                                     OrderDirection createdAtOrder) {
    List<Order> orders = new ArrayList<>();
    if (descriptionOrder != null) {
      orders.add(new Order(Direction.valueOf(descriptionOrder.getValue()), "description"));
    }
    if (unitPriceOrder != null) {
      orders.add(new Order(Direction.valueOf(unitPriceOrder.getValue()), "unitPrice"));
    }
    if (createdAtOrder != null) {
      orders.add(new Order(Direction.valueOf(createdAtOrder.getValue()), "createdAt"));
    }
    return orders;
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
