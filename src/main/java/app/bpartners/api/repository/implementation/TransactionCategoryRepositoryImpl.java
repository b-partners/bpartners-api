package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.mapper.TransactionCategoryMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionCategoryRepositoryImpl implements TransactionCategoryRepository {
  private final TransactionCategoryJpaRepository jpaRepository;
  private final TransactionCategoryMapper mapper;

  @Override
  public List<TransactionCategory> findAll() {
    return jpaRepository.findAll().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<TransactionCategory> saveAll(List<TransactionCategory> toCreate) {
    List<HTransactionCategory> entities = toCreate.stream()
        .map(mapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entities).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }
}
