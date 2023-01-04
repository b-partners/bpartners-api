package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.CustomerRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.Customer;
import app.bpartners.api.service.CustomerService;
import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
      @RequestParam(required = false) String name) {
    return service.getCustomers(id, name).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/accounts/{id}/customers")
  public List<app.bpartners.api.endpoint.rest.model.Customer> createCustomers(
      @PathVariable String id,
      @RequestBody List<CreateCustomer> toCreate) {
    List<Customer> customers = toCreate.stream()
        .map(createCustomer -> mapper.toDomain(id, createCustomer))
        .collect(Collectors.toUnmodifiableList());
    return service.crupdateCustomers(id, customers).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping("/accounts/{id}/customers")
  public List<app.bpartners.api.endpoint.rest.model.Customer> updateCustomers(
      @PathVariable("id") String id,
      @RequestBody List<app.bpartners.api.endpoint.rest.model.Customer> toUpdate) {
    List<Customer> customers = toUpdate.stream()
        .map(customer -> mapper.toDomain(id, customer))
        .collect(Collectors.toUnmodifiableList());
    return service.crupdateCustomers(id, customers).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping(value = "/accounts/{accountId}/customers/upload")
  public List<app.bpartners.api.endpoint.rest.model.Customer> uploadCustomersInfo(
      @AuthenticationPrincipal Principal principal,
      @PathVariable(name = "accountId") String accountId,
      @RequestBody File toUpload) {
    List<Customer> customerTemplates = service.getDataFromFile(toUpload)
        .stream().map(customer -> mapper.toDomain(accountId, customer))
        .collect(Collectors.toUnmodifiableList());
    return service.crupdateCustomers(accountId, customerTemplates)
        .stream().map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
