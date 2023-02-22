package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.CustomerRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.PageFromOne;
import app.bpartners.api.service.CustomerService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class CustomerController {
  private final CustomerService service;
  private final CustomerRestMapper mapper;

  @GetMapping("/accounts/{id}/customers")
  public List<app.bpartners.api.endpoint.rest.model.Customer> getCustomers(
      @PathVariable String id,
      @RequestParam(required = false) String name,
      @RequestParam(required = false) String firstName,
      @RequestParam(required = false) String lastName,
      @RequestParam(required = false) PageFromOne page,
      @RequestParam(required = false) BoundedPageSize pageSize) {
    return service.getCustomers(id, name, firstName, lastName, page, pageSize).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/accounts/{id}/customers")
  public List<app.bpartners.api.endpoint.rest.model.Customer> createCustomers(
      @PathVariable String id,
      @RequestBody List<CreateCustomer> toCreate) {
    List<app.bpartners.api.model.Customer> customers = toCreate.stream()
        .map(createCustomer -> mapper.toDomain(id, createCustomer))
        .collect(Collectors.toUnmodifiableList());
    return service.crupdateCustomers(id, customers).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping("/accounts/{id}/customers")
  public List<app.bpartners.api.endpoint.rest.model.Customer> updateCustomers(
      @PathVariable("id") String id,
      @RequestBody List<Customer> toUpdate) {
    List<app.bpartners.api.model.Customer> customers = toUpdate.stream()
        .map(customer -> mapper.toDomain(id, customer))
        .collect(Collectors.toUnmodifiableList());
    return service.crupdateCustomers(id, customers).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping(value = "/accounts/{accountId}/customers/upload")
  public List<Customer> importCustomers(
      @PathVariable(name = "accountId") String accountId,
      @RequestBody byte[] toUpload) {
    List<app.bpartners.api.model.Customer> customerTemplates =
        service.getDataFromFile(accountId, toUpload);
    return service.crupdateCustomers(accountId, customerTemplates)
        .stream().map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

}
