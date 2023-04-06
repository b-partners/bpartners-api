package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.mapper.ProductRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.OrderDirection;
import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.model.Fraction;
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

@Service
@AllArgsConstructor
public class ProductService {
  private final ProductRepository repository;
  private final ProductRestMapper restMapper;

  public List<Product> getProductsByAccount(
      String accountId, ProductStatus status, int page, int pageSize,
      OrderDirection descriptionOrder, OrderDirection unitPriceOrder, OrderDirection createdAtOrder,
      String description, Integer unitPrice) {
    Fraction productUnitPrice = unitPrice == null ? null : parseFraction(unitPrice);
    return repository.findAllByIdAccountAndStatusAndOrByDescriptionAndOrUnitPrice(accountId, status,
        page, pageSize, descriptionOrder, unitPriceOrder, createdAtOrder, description,
        productUnitPrice);
  }

  public List<Product> crupdate(String accountId, List<Product> toCreate) {
    return repository.saveAll(accountId, removeDuplicatedProducts(toCreate));
  }

  public List<Product> getDataFromFile(String accountId, byte[] file) {
    List<CreateProduct> productsFromFile =
        getProductsFromFile(new ByteArrayInputStream(file)).stream()
            .distinct()
            .collect(Collectors.toUnmodifiableList());
    return checkPersistence(accountId, productsFromFile);
  }

  public List<Product> removeDuplicatedProducts(List<Product> list) {
    for (int i = 0; i < list.size() - 1; i++) {
      Product currentProduct = list.get(i);
      for (int j = i + 1; j < list.size(); j++) {
        Product nextProduct = list.get(j);
        if (currentProduct.getDescription().equals(nextProduct.getDescription())) {
          list.remove(j);
        }
      }
    }
    return list;
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
      toCreateList = createProducts.stream().map(restMapper::toDomain)
          .collect(Collectors.toUnmodifiableList());
    } else {
      List<String> toUpdateDescription = toUpdateList.stream().map(Product::getDescription)
          .collect(Collectors.toUnmodifiableList());
      toCreateList = createProducts.stream()
          .filter(createProduct -> !toUpdateDescription.contains(createProduct.getDescription()))
          .map(restMapper::toDomain).collect(Collectors.toUnmodifiableList());
    }
    toUpdateList.addAll(toCreateList);
    return toUpdateList;
  }

  public List<Product> saveAll(String accountId, List<Product> toSave) {
    return repository.saveAll(accountId, toSave);
  }
}
