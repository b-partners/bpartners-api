package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.model.Customer;
import java.util.List;

public interface CustomerRepository {
  List<Customer> saveAll(List<Customer> toCreate);

  Customer findById(String id);

  List<Customer> findByIdUserAndCriteria(
      String accountId, String firstname, String lastname, String email, String phoneNumber,
      String city, String country, CustomerStatus status, int page, int pageSize);

  List<Customer> updateCustomersStatuses(List<UpdateCustomerStatus> toUpdate);
}
