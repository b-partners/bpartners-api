package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.CustomerRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.service.CustomerService;
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
public class CustomerController {
  private final CustomerService service;
  private final CustomerRestMapper mapper;

  @GetMapping("/accounts/{id}/customers")
  public List<Customer> getCustomers(
      @PathVariable String id,
      @RequestParam(required = false) String name) {
    return service.getCustomers(id, name).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/accounts/{id}/customers")
  public List<Customer> createCustomers(
      @PathVariable String id,
      @RequestBody List<CreateCustomer> toCreate) {
    List<app.bpartners.api.model.Customer> customers = toCreate.stream()
        .map(createCustomer -> mapper.toDomain(id, createCustomer))
        .collect(Collectors.toUnmodifiableList());
    return service.createCustomers(id, customers).stream()
        .map(mapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
