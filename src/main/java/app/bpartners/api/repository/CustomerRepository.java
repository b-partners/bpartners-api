package app.bpartners.api.repository;

import app.bpartners.api.model.Customer;
import java.util.List;

public interface CustomerRepository {

  List<Customer> findByAccountIdAndName(String accountId, String name);

  List<Customer> findByAccount(String accountId);

  List<Customer> save(String account, List<Customer> toCreate);
}
