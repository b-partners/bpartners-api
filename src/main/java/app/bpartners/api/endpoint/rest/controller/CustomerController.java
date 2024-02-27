package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.CustomerService.EXCEL_MIME_TYPE;
import static app.bpartners.api.service.CustomerService.TEXT_CSV_MIME_TYPE;

import app.bpartners.api.endpoint.rest.mapper.CustomerRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.validator.UpdateCustomerStatusValidator;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.service.CustomerService;
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
public class CustomerController {
  public static final String CSV_EXTENSION = ".csv";
  public static final String EXCEL_EXTENSION = ".xlsx";
  private final CustomerService service;
  private final CustomerRestMapper mapper;
  private final UpdateCustomerStatusValidator validator;

  @GetMapping(value = "/accounts/{aId}/customers/export")
  public void exportCustomers(
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
          "Content-Disposition", "attachment; filename=\"customers" + fileExtension + "\"");
      response.setCharacterEncoding("UTF-8");
      PrintWriter writer = response.getWriter();

      service.exportCustomers(idUser, fileType, writer);

      writer.close();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  @GetMapping("/accounts/{id}/customers")
  // TODO: only filters should be used for filtering customers
  public List<app.bpartners.api.endpoint.rest.model.Customer> getCustomers(
      @PathVariable String id,
      @RequestParam(required = false) String firstName,
      @RequestParam(required = false) String lastName,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String phoneNumber,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String country,
      @RequestParam(required = false) List<String> filters,
      @RequestParam(required = false) CustomerStatus status,
      @RequestParam(required = false) PageFromOne page,
      @RequestParam(required = false) BoundedPageSize pageSize) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    return service
        .getCustomers(
            idUser,
            firstName,
            lastName,
            email,
            phoneNumber,
            city,
            country,
            filters,
            status,
            page,
            pageSize)
        .stream()
        .map(mapper::toRest)
        .toList();
  }

  @GetMapping("/accounts/{aId}/customers/{cId}")
  public Customer getUniqueCustomer(
      @PathVariable(name = "aId") String accountId, @PathVariable(name = "cId") String id) {
    return mapper.toRest(service.getCustomerById(id));
  }

  @PostMapping("/accounts/{id}/customers")
  public List<Customer> createCustomers(
      @PathVariable(name = "id") String idAccount, @RequestBody List<CreateCustomer> toCreate) {
    log.warn("POST /accounts/{id}/customers is deprecated. Use PUT instead");
    String idUser =
        AuthProvider.getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    List<app.bpartners.api.model.Customer> customers =
        toCreate.stream().map(createCustomer -> mapper.toDomain(idUser, createCustomer)).toList();
    return service.crupdateCustomers(customers).stream().map(mapper::toRest).toList();
  }

  @PutMapping("/accounts/{id}/customers")
  public List<Customer> crupdateCustomers(
      @PathVariable("id") String id, @RequestBody List<Customer> toUpdate) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    List<app.bpartners.api.model.Customer> customers =
        toUpdate.stream().map(customer -> mapper.toDomain(idUser, customer)).toList();
    return service.crupdateCustomers(customers).stream().map(mapper::toRest).toList();
  }

  @PostMapping(value = "/accounts/{accountId}/customers/upload")
  public List<Customer> importCustomers(
      @PathVariable(name = "accountId") String accountId, @RequestBody byte[] toUpload) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); // TODO: should be changed when endpoint changed
    List<app.bpartners.api.model.Customer> customerTemplates =
        service.getDataFromFile(idUser, toUpload);
    return service.crupdateCustomers(customerTemplates).stream().map(mapper::toRest).toList();
  }

  @PutMapping(value = "/accounts/{id}/customers/status")
  public List<Customer> updateCustomerStatus(
      @PathVariable(name = "id") String accountId,
      @RequestBody List<UpdateCustomerStatus> customerStatuses) {
    validator.accept(customerStatuses);
    return service.updateStatuses(customerStatuses).stream().map(mapper::toRest).toList();
  }
}
