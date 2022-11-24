package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.InvoiceRestMapper;
import app.bpartners.api.endpoint.rest.mapper.ProductRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.endpoint.rest.model.Invoice;
import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.service.InvoiceService;
import app.bpartners.api.service.ProductService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class ProductController {
  private final ProductRestMapper mapper;
  private final ProductService productService;
  private final InvoiceService invoiceService;
  private final InvoiceRestMapper invoiceMapper;


  @GetMapping("/accounts/{id}/products")
  public List<Product> getProducts(
      @PathVariable String id,
      @RequestParam(required = false) Boolean unique,
      @RequestParam(required = false) String description
  ) {
    return productService.getProductsByAccount(id, description, unique).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  //TODO: do not attach the products to an invoice
  @PostMapping("/accounts/{aId}/invoices/{iId}/products")
  public Invoice createProducts(
      @PathVariable(name = "aId") String accountId,
      @PathVariable(name = "iId") String invoiceId,
      @RequestBody List<CreateProduct> toCreate) {
    List<app.bpartners.api.model.Product> domainToCreate = toCreate.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
    productService.createProducts(accountId, invoiceId, domainToCreate);
    return null; //to remove and return product instead
  }
}
