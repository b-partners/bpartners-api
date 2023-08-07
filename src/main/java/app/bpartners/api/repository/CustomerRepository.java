package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.model.Customer;
import java.util.List;
import java.util.Optional;

public interface CustomerRepository {
  List<Customer> findByIdAccountHolder(String idAccountHolder);

  List<Customer> saveAll(List<Customer> toCreate);

  Customer save(Customer toSave);

  Customer findById(String id);

  Optional<Customer> findOptionalById(String id);

  List<Customer> findByIdUserAndCriteria(
      String accountId, String firstName, String lastName, String email,
      String phoneNumber, String city, String country,
      List<String> keywords, CustomerStatus status, int page, int pageSize);

  List<Customer> updateCustomersStatuses(List<UpdateCustomerStatus> toUpdate);

  List<Customer> findWhereLatitudeOrLongitudeIsNull();
}
