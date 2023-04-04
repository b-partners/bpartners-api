package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.model.Customer;
import java.util.List;

public interface CustomerRepository {

  List<Customer> findByAccountIdAndName(
      String accountId, String firstName, String lastName, int page, int pageSize);

  List<Customer> findByAccount(String accountId, int page, int pageSize);

  List<Customer> saveAll(String account, List<Customer> toCreate);

  Customer findById(String id);

  List<Customer> findByAccountIdAndCriteria(
      String accountId, String firstname, String lastname, String email, String phoneNumber,
      String city, String country, CustomerStatus status, int page, int pageSize);

  List<Customer> updateStatus(String accountId, List<UpdateCustomerStatus> toUpdate);
}
