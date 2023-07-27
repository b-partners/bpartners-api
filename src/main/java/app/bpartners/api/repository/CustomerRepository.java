package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.model.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
  List<Customer> saveAll(List<Customer> toCreate);

  Customer findById(String id);

  Optional<Customer> findOptionalById(String id);

  List<Customer> findByIdUserAndCriteria(
      String accountId, List<String> keywords, CustomerStatus status, int page, int pageSize);

  List<Customer> updateCustomersStatuses(List<UpdateCustomerStatus> toUpdate);
}
