package app.bpartners.api.repository;

import app.bpartners.api.model.CustomerTemplate;
import java.util.List;

public interface CustomerRepository {

  List<CustomerTemplate> findByAccountIdAndName(String accountId, String name);

  List<CustomerTemplate> findByAccount(String accountId);

  List<CustomerTemplate> save(String account, List<CustomerTemplate> toCreate);

  CustomerTemplate findById(String id);
}
