package app.bpartners.api.repository;

import app.bpartners.api.model.Customer;
import java.util.List;

public interface CustomerRepository {

  List<Customer> findByAccountIdAndName(String accountId, String firstName, String lastName);

  List<Customer> findByAccount(String accountId);

  List<Customer> saveAll(String account, List<Customer> toCreate);

  Customer findById(String id);

}
