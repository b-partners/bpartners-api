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

  private List<Order> retrieveOrders(
      OrderDirection descriptionOrder,
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
  public List<Product> findAllByIdUserOrderByDescriptionAsc(String idUser) {
    return jpaRepository.findByIdUserOrderByDescriptionAsc(idUser).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public List<Product> saveAll(String idUser, List<Product> toCreate) {
    List<HProduct> entityToCreate =
        toCreate.stream()
            .map(customer -> checkExisting(idUser, customer))
            .map(customer -> mapper.toEntity(idUser, customer))
            .collect(Collectors.toList());
    return jpaRepository.saveAll(entityToCreate).stream().map(mapper::toDomain).toList();
  }

  // TODO: replace to findByCriteria as in Customers
  @Override
  public List<Product> findByIdUserAndCriteria(
      String idUser,
      ProductStatus status,
      Integer page,
      Integer pageSize,
      OrderDirection descriptionOrder,
      OrderDirection unitPriceOrder,
      OrderDirection createdAtOrder,
      String description,
      Fraction unitPrice) {
    List<Order> orders = retrieveOrders(descriptionOrder, unitPriceOrder, createdAtOrder);
    Pageable pageRequest = PageRequest.of(page, pageSize, Sort.by(orders));
    String descriptionFilter = description == null ? "" : description;
    String priceFilter = unitPrice == null ? "" : String.valueOf(unitPrice);
    List<HProduct> products =
        unitPrice == null
            ? jpaRepository.findAllByIdUserAndStatusAndDescriptionContainingIgnoreCase(
                idUser, status, descriptionFilter, pageRequest)
            : jpaRepository.findAllByIdUserAndStatusAndUnitPriceAndDescriptionContainingIgnoreCase(
                idUser, status, priceFilter, descriptionFilter, pageRequest);

    return products.stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<Product> updateStatuses(List<UpdateProductStatus> productStatuses) {
    List<HProduct> products =
        productStatuses.stream()
            .map(
                productStatus ->
                    jpaRepository
                        .findById(productStatus.getId())
                        .orElseThrow(
                            () ->
                                new NotFoundException(
                                    notFoundExceptionMessage(productStatus.getId())))
                        .toBuilder()
                        .status(productStatus.getStatus())
                        .build())
            .collect(Collectors.toList());
    return jpaRepository.saveAll(products).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public Product findById(String id) {
    return mapper.toDomain(
        jpaRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException(notFoundExceptionMessage(id))));
  }

  private Product checkExisting(String idUser, Product domain) {
    String id = domain.getId();
    if (id != null) {
      jpaRepository
          .findById(id)
          .orElseThrow(
              () ->
                  new NotFoundException(
                      notFoundExceptionMessage(id) + " for User(id=" + idUser + ")"));
    }
    Optional<HProduct> optionalProduct =
        jpaRepository.findByIdUserAndDescription(idUser, domain.getDescription());
    return optionalProduct.isEmpty()
        ? domain
        : domain.toBuilder().id(optionalProduct.get().getId()).build();
  }

  private String notFoundExceptionMessage(String id) {
    return "Product(id=" + id + ") not found";
  }
}
