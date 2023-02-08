package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.ProductRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.Product;
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


  @GetMapping("/accounts/{id}/products")
  public List<Product> getProducts(
      @PathVariable String id,
      @RequestParam(required = false) PageFromOne page,
      @RequestParam(required = false) BoundedPageSize pageSize) {
    int pageValue = page == null ? 0
        : page.getValue() - 1;
    int pageSizeValue = pageSize == null ? 50
        : pageSize.getValue();
    return productService.getProductsByAccount(id, pageValue, pageSizeValue).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping("/accounts/{aId}/products")
  public List<Product> crupdateProducts(
      @PathVariable(name = "aId") String accountId,
      @RequestBody List<CreateProduct> toCreate) {
    List<app.bpartners.api.model.Product> domainToCreate = toCreate.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    return productService.createOrUpdateProducts(accountId, domainToCreate).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/accounts/{aId}/products")
  public List<Product> createProducts(
      @PathVariable(name = "aId") String accountId,
      @RequestBody List<CreateProduct> toCreate) {
    log.warn("This method is deprecated, use put instead");
    List<app.bpartners.api.model.Product> domainToCreate = toCreate.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    return productService.createProducts(accountId, domainToCreate).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
