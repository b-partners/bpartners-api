package app.bpartners.api.repository;

import app.bpartners.api.model.Bank;

public interface BankRepository {
  Bank findById(Integer id);
}
