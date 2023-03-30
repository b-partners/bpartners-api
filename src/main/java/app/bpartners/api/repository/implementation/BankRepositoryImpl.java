package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Bank;
import app.bpartners.api.model.mapper.BankMapper;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.bridge.repository.BridgeBankRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class BankRepositoryImpl implements BankRepository {
  private final BridgeBankRepository bridgeRepository;
  private final BankMapper mapper;

  @Override
  public Bank findById(Integer id) {
    return mapper.toDomain(bridgeRepository.findById(id));
  }
}
