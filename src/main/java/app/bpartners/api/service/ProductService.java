package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.mapper.ProductRestMapper;
import app.bpartners.api.endpoint.rest.model.OrderDirection;
import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.endpoint.rest.model.UpdateProductStatus;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Product;
import app.bpartners.api.repository.ProductRepository;
import java.io.ByteArrayInputStream;
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
    return repository.findAllByIdAccountAndStatusAndOrByDescriptionAndOrUnitPrice(accountId,
        status == null ? ProductStatus.ENABLED : status, page, pageSize, descriptionOrder,
        unitPriceOrder, createdAtOrder, description, productUnitPrice);
  }

  public List<Product> crupdate(String accountId, List<Product> toCreate) {
    return repository.saveAll(accountId, toCreate);
  }

  public List<Product> updateStatus(String accountId, List<UpdateProductStatus> toUpdate) {
    return repository.updateStatus(accountId, toUpdate);
  }

  public List<Product> getDataFromFile(String accountId, byte[] file) {
    return getProductsFromFile(new ByteArrayInputStream(file)).stream()
        .map(restMapper::toDomain)
        .collect(Collectors.toList());
  }
}
