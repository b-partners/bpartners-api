package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.OrderDirection;
import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.endpoint.rest.model.UpdateProductStatus;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Product;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.ProductMapper;
import app.bpartners.api.repository.ProductRepository;
import app.bpartners.api.repository.jpa.ProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HProduct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class ProductRepositoryImpl implements ProductRepository {
  private final ProductJpaRepository jpaRepository;
  private final ProductMapper mapper;

  private static Order defaultOrder() {
    return new Order(Direction.DESC, "createdAt");
  }

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

  @Override
  public List<Product> findAllByIdAccount(String idAccount) {
    return jpaRepository.findAllByIdAccount(idAccount).stream()
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
    if (orders.isEmpty()) {
      orders.add(defaultOrder());
    }
    return orders;
  }

  @Override
  public List<Product> saveAll(String accountId, List<Product> toCreate) {
    List<HProduct> entityToCreate = toCreate.stream()
        .map(customer -> checkExisting(accountId, customer))
        .map(customer -> mapper.toEntity(accountId, customer))
        .collect(Collectors.toList());
    return jpaRepository.saveAll(entityToCreate).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Product> findAllByIdAccountAndStatusAndOrByDescriptionAndOrUnitPrice(
      String idAccount, ProductStatus status, Integer page, Integer pageSize,
      OrderDirection descriptionOrder, OrderDirection unitPriceOrder,
      OrderDirection createdAtOrder,
      String description, Fraction unitPrice) {
    List<Order> orders = retrieveOrders(descriptionOrder, unitPriceOrder, createdAtOrder);
    Pageable pageRequest = PageRequest.of(page, pageSize, Sort.by(orders));
    String descriptionFilter = description == null ? "" : description;
    String priceFilter = unitPrice == null ? "" : String.valueOf(unitPrice);
    List<HProduct> products = unitPrice == null
        ? jpaRepository.findAllByIdAccountAndStatusAndDescriptionContainingIgnoreCase(
        idAccount, status, descriptionFilter, pageRequest)
        : jpaRepository.findAllByIdAccountAndStatusAndUnitPriceAndDescriptionContainingIgnoreCase(
        idAccount, status, priceFilter, descriptionFilter, pageRequest);

    return products.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Product> updateStatus(String accountId, List<UpdateProductStatus> toUpdate) {
    List<Product> productUpdated = new ArrayList<>();
    for (UpdateProductStatus product : toUpdate) {
      HProduct existingProduct = jpaRepository.findByIdAccountAndId(accountId, product.getId());
      existingProduct.setStatus(product.getStatus());
      productUpdated.add(mapper.toDomain(jpaRepository.save(existingProduct)));
    }
    return productUpdated;
  }

  private Product checkExisting(String accountId, Product domain) {
    String id = domain.getId();
    if (id != null) {
      jpaRepository.findById(id)
          .orElseThrow(()
              -> new NotFoundException(
              "Product(id=" + id + ") not found for Account(id=" + accountId + ")"));
    }
    Optional<HProduct> optionalProduct = jpaRepository.findByIdAccountAndDescription(
        accountId, domain.getDescription());
    return optionalProduct.isEmpty() ? domain : domain.toBuilder()
        .id(optionalProduct.get().getId())
        .build();
  }
}
