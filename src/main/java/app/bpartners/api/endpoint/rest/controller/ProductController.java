package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.ProductRestMapper;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.service.ProductService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ProductController {
  private final ProductRestMapper mapper;
  private final ProductService service;

  @GetMapping("/accounts/{id}/products")
  public List<Product> getProducts(
      @PathVariable String id,
      @RequestParam(required = false) Boolean unique,
      @RequestParam(required = false) String description
  ) {
    return service.getProductsByAccount(id, description, unique).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
