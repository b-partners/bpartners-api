package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.CustomerRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.validator.UpdateCustomerStatusValidator;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.CustomerService;
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
public class CustomerController {
  private final CustomerService service;
  private final CustomerRestMapper mapper;
  private final UpdateCustomerStatusValidator validator;

  @GetMapping("/accounts/{id}/customers")
  public List<app.bpartners.api.endpoint.rest.model.Customer> getCustomers(
      @PathVariable String id,
      @RequestParam(required = false) String firstName,
      @RequestParam(required = false) String lastName,
      @RequestParam(required = false) String email,
      @RequestParam(required = false) String phoneNumber,
      @RequestParam(required = false) String city,
      @RequestParam(required = false) String country,
      @RequestParam(required = false) CustomerStatus status,
      @RequestParam(required = false) PageFromOne page,
      @RequestParam(required = false) BoundedPageSize pageSize) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); //TODO: should be changed when endpoint changed
    return service.getCustomers(
            idUser, firstName, lastName, email, phoneNumber,
            city, country, status, page, pageSize).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @GetMapping("/accounts/{aId}/customers/{cId}")
  public Customer getUniqueCustomer(
      @PathVariable(name = "aId") String accountId,
      @PathVariable(name = "cId") String id) {
    return mapper.toRest(service.getCustomerById(id));
  }

  @PostMapping("/accounts/{id}/customers")
  public List<Customer> createCustomers(
      @PathVariable(name = "id") String idAccount,
      @RequestBody List<CreateCustomer> toCreate) {
    log.warn("POST /accounts/{id}/customers is deprecated. Use PUT instead");
    String idUser =
        AuthProvider.getAuthenticatedUserId(); //TODO: should be changed when endpoint changed
    List<app.bpartners.api.model.Customer> customers = toCreate.stream()
        .map(createCustomer -> mapper.toDomain(idUser, createCustomer))
        .collect(Collectors.toUnmodifiableList());
    return service.crupdateCustomers(customers).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping("/accounts/{id}/customers")
  public List<Customer> crupdateCustomers(
      @PathVariable("id") String id,
      @RequestBody List<Customer> toUpdate) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); //TODO: should be changed when endpoint changed
    List<app.bpartners.api.model.Customer> customers = toUpdate.stream()
        .map(customer -> mapper.toDomain(idUser, customer))
        .collect(Collectors.toUnmodifiableList());
    return service.crupdateCustomers(customers).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping(value = "/accounts/{accountId}/customers/upload")
  public List<Customer> importCustomers(
      @PathVariable(name = "accountId") String accountId,
      @RequestBody byte[] toUpload) {
    String idUser =
        AuthProvider.getAuthenticatedUserId(); //TODO: should be changed when endpoint changed
    List<app.bpartners.api.model.Customer> customerTemplates =
        service.getDataFromFile(idUser, toUpload);
    return service.crupdateCustomers(customerTemplates)
        .stream().map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping(value = "/accounts/{id}/customers/status")
  public List<Customer> updateCustomerStatus(
      @PathVariable(name = "id") String accountId,
      @RequestBody List<UpdateCustomerStatus> customerStatuses) {
    validator.accept(customerStatuses);
    return service.updateStatuses(customerStatuses).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
