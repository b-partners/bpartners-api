package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.ProductRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.OrderDirection;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.endpoint.rest.model.ProductStatus;
import app.bpartners.api.endpoint.rest.model.UpdateProductStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.validator.UpdateProductStatusValidator;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.ProductService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class ProductController {
  private final ProductRestMapper mapper;
  private final ProductService productService;
  private final UpdateProductStatusValidator validator;

  @GetMapping("/accounts/{id}/products")
  public List<Product> getProducts(
      @PathVariable String id,
      @RequestParam(required = false) PageFromOne page,
      @RequestParam(required = false) BoundedPageSize pageSize,
      @RequestParam(required = false, name = "descriptionOrder") OrderDirection descriptionOrder,
      @RequestParam(required = false, name = "unitPriceOrder") OrderDirection unitPriceOrder,
      @RequestParam(required = false, name = "createdAtOrder") OrderDirection createdDateOrder,
      @RequestParam(required = false, name = "descriptionFilter") String description,
      @RequestParam(required = false, name = "priceFilter") Integer unitPrice,
      @RequestParam(required = false, name = "status") ProductStatus status) {
    int pageValue = page == null ? 0
        : page.getValue() - 1;
    int pageSizeValue = pageSize == null ? 50
        : pageSize.getValue();
    String idUser =
        AuthProvider.getAuthenticatedUserId(); //TODO: should be changed when endpoint changed
    return productService.getByIdUserAndCriteria(idUser, status, pageValue, pageSizeValue,
            descriptionOrder, unitPriceOrder, createdDateOrder, description, unitPrice)
        .stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping("/accounts/{aId}/products")
  public List<Product> crupdateProducts(
      @PathVariable(name = "aId") String accountId,
      @RequestBody List<CreateProduct> toCrupdate) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); //TODO: should be changed when endpoint changed
    List<app.bpartners.api.model.Product> domain = toCrupdate.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    return productService.crupdate(idUser, domain).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/accounts/{aId}/products")
  public List<Product> createProducts(
      @PathVariable(name = "aId") String accountId,
      @RequestBody List<CreateProduct> toCrupdate) {
    log.warn("DEPRECATED: POST method is still used for crupdating products."
        + " Use PUT instead");
    String idUser =
        AuthProvider.getAuthenticatedUserId(); //TODO: should be changed when endpoint changed
    List<app.bpartners.api.model.Product> domain = toCrupdate.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    return productService.crupdate(idUser, domain).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping("/accounts/{aId}/products/status")
  public List<Product> updateStatuses(
      @PathVariable(name = "aId") String accountId,
      @RequestBody List<UpdateProductStatus> productStatuses) {
    validator.accept(productStatuses);
    return productService.updateStatuses(productStatuses).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping(value = "/accounts/{accountId}/products/upload")
  public List<Product> uploadProductsList(
      @PathVariable(name = "accountId") String accountId,
      @RequestBody byte[] toUpload) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); //TODO: should be changed when endpoint changed
    List<app.bpartners.api.model.Product> products =
        productService.getDataFromFile(toUpload);
    return productService.crupdate(idUser, products)
        .stream().map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
