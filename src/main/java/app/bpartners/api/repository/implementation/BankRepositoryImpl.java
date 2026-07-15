package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Bank;
import app.bpartners.api.model.mapper.BankMapper;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.jpa.BankJpaRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@AllArgsConstructor
public class BankRepositoryImpl implements BankRepository {
  private final BankMapper mapper;
  private final BankJpaRepository jpaRepository;

  @Override
  public Bank findById(String id) {
    if (id == null) {
      return null;
    }
    return mapper.toDomain(jpaRepository.findById(id).orElse(null));
  }
}
