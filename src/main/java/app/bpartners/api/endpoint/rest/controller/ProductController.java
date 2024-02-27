package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.CustomerService.EXCEL_MIME_TYPE;
import static app.bpartners.api.service.CustomerService.TEXT_CSV_MIME_TYPE;

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
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.service.ProductService;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
@Slf4j
public class ProductController {
  public static final String CSV_EXTENSION = ".csv";
  public static final String EXCEL_EXTENSION = ".xlsx";
  private final ProductRestMapper mapper;
  private final ProductService service;
  private final UpdateProductStatusValidator validator;

  @GetMapping(value = "/accounts/{aId}/products/export")
  public void exportProducts(
      @PathVariable String aId,
      @RequestHeader("Accept") String fileType,
      HttpServletResponse response) {
    if (!fileType.equals(TEXT_CSV_MIME_TYPE)) {
      throw new NotImplementedException("Only CSV export file is supported for now");
    }
    String idUser = AuthProvider.getAuthenticatedUserId();
    try {
      String fileExtension =
          fileType.equals(TEXT_CSV_MIME_TYPE)
              ? CSV_EXTENSION
              : (fileType.equals(EXCEL_MIME_TYPE) ? EXCEL_EXTENSION : null);
      response.setContentType(fileType);
      response.setHeader(
          "Content-Disposition", "attachment; filename=\"products" + fileExtension + "\"");
      response.setCharacterEncoding("UTF-8");
      PrintWriter writer = response.getWriter();

      service.exportCustomers(idUser, writer);

      writer.close();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

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
    int pageValue = page == null ? 0 : page.getValue() - 1;
    int pageSizeValue = pageSize == null ? 50 : pageSize.getValue();
    String idUser =
        AuthProvider.getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    return service
        .getByIdUserAndCriteria(
            idUser,
            status,
            pageValue,
            pageSizeValue,
            descriptionOrder,
            unitPriceOrder,
            createdDateOrder,
            description,
            unitPrice)
        .stream()
        .map(mapper::toRest)
        .toList();
  }

  @GetMapping("/accounts/{aId}/products/{pId}")
  public Product getUniqueProduct(
      @PathVariable(name = "aId") String accountId, @PathVariable(name = "pId") String id) {
    return mapper.toRest(service.getById(id));
  }

  @PutMapping("/accounts/{aId}/products")
  public List<Product> crupdateProducts(
      @PathVariable(name = "aId") String accountId, @RequestBody List<CreateProduct> toCrupdate) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    List<app.bpartners.api.model.Product> domain =
        toCrupdate.stream().map(mapper::toDomain).toList();
    return service.crupdate(idUser, domain).stream().map(mapper::toRest).toList();
  }

  @PostMapping("/accounts/{aId}/products")
  public List<Product> createProducts(
      @PathVariable(name = "aId") String accountId, @RequestBody List<CreateProduct> toCrupdate) {
    log.warn("DEPRECATED: POST method is still used for crupdating products." + " Use PUT instead");
    String idUser =
        AuthProvider.getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    List<app.bpartners.api.model.Product> domain =
        toCrupdate.stream().map(mapper::toDomain).toList();
    return service.crupdate(idUser, domain).stream().map(mapper::toRest).toList();
  }

  @PutMapping("/accounts/{aId}/products/status")
  public List<Product> updateStatuses(
      @PathVariable(name = "aId") String accountId,
      @RequestBody List<UpdateProductStatus> productStatuses) {
    validator.accept(productStatuses);
    return service.updateStatuses(productStatuses).stream().map(mapper::toRest).toList();
  }

  @PostMapping(value = "/accounts/{accountId}/products/upload")
  public List<Product> uploadProductsList(
      @PathVariable(name = "accountId") String accountId, @RequestBody byte[] toUpload) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    List<app.bpartners.api.model.Product> products = service.getDataFromFile(toUpload);
    return service.crupdate(idUser, products).stream().map(mapper::toRest).toList();
  }
}
