package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.TransactionCategoryType;
import app.bpartners.api.model.mapper.TransactionCategoryTypeMapper;
import app.bpartners.api.repository.TransactionCategoryTypeRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryTypeJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategoryType;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionCategoryRepositoryTypeImpl implements TransactionCategoryTypeRepository {
  private final TransactionCategoryTypeJpaRepository jpaRepository;
  private final TransactionCategoryTypeMapper mapper;

  @Override
  public List<TransactionCategoryType> findAll() {
    return jpaRepository.findAll().stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<TransactionCategoryType> saveAll(List<TransactionCategoryType> toCreate) {
    List<HTransactionCategoryType> entities = toCreate.stream()
        .map(mapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entities).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public TransactionCategoryType findById(String id) {
    return mapper.toDomain(jpaRepository.getById(id));
  }
}