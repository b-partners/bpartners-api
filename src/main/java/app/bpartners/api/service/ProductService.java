package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.OrderDirection;
import app.bpartners.api.endpoint.rest.model.UpdateProductStatus;
import app.bpartners.api.model.Product;
import app.bpartners.api.repository.ProductRepository;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static app.bpartners.api.service.utils.ProductUtils.getProductsFromFile;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
public class ProductService {
  private final ProductRepository repository;

  public List<Product> getProductsByAccount(
      String accountId, int page, int pageSize,
      OrderDirection descriptionOrder,
      OrderDirection unitPriceOrder,
      OrderDirection createdAtOrder) {
    return repository.findAllByIdAccountAndStatus(accountId, page, pageSize,
        descriptionOrder, unitPriceOrder, createdAtOrder);
  }

  public List<Product> crupdate(String accountId, List<Product> toCreate) {
    return repository.saveAll(accountId, toCreate);
  }

  public List<Product> updateStatus(String accountId, List<UpdateProductStatus> toUpdate) {
    return repository.updateStatus(accountId, toUpdate);
  }

  public List<Product> getDataFromFile(String accountId, byte[] file) {
    List<CreateProduct> productsFromFile = getProductsFromFile(new ByteArrayInputStream(file))
        .stream().distinct()
        .collect(Collectors.toUnmodifiableList());
    return checkPersistence(accountId, productsFromFile);
  }

  private List<Product> checkPersistence(String accountId, List<CreateProduct> createProducts) {
    List<Product> toUpdateList = new ArrayList<>();
    List<Product> toCreateList;
    List<Product> persisted = repository.findAllByIdAccount(accountId);
    for (Product product : persisted) {
      for (CreateProduct toCreate : createProducts) {
        if (product.getDescription().equals(toCreate.getDescription())) {
          product.setUnitPrice(parseFraction(toCreate.getUnitPrice()));
          product.setVatPercent(parseFraction(toCreate.getVatPercent()));
          toUpdateList.add(product);
        }
      }
    }
    if (toUpdateList.isEmpty()) {
      toCreateList = createProducts.stream()
          .map(this::toDomainWithoutCheck)
          .collect(Collectors.toUnmodifiableList());
    } else {
      List<String> toUpdateDescription =
          toUpdateList.stream().map(Product::getDescription).collect(
              Collectors.toUnmodifiableList());
      toCreateList = createProducts.stream()
          .filter(createProduct -> !toUpdateDescription.contains(createProduct.getDescription()))
          .map(this::toDomainWithoutCheck)
          .collect(Collectors.toUnmodifiableList());
    }
    toUpdateList.addAll(toCreateList);
    return toUpdateList;
  }

  //TODO: put this in the ProductRestMapper
  private Product toDomainWithoutCheck(CreateProduct createProduct) {
    return Product.builder()
        .id(createProduct.getId() == null
            ? String.valueOf(randomUUID())
            : createProduct.getId())
        .description(createProduct.getDescription())
        .unitPrice(parseFraction(createProduct.getUnitPrice()))
        .vatPercent(parseFraction(createProduct.getVatPercent()))
        .build();
  }
}
