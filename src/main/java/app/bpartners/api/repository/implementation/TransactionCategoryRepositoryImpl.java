package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.mapper.TransactionCategoryMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionCategoryRepositoryImpl implements TransactionCategoryRepository {
  private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2000, 1, 1);
  private final TransactionCategoryJpaRepository jpaRepository;
  private final TransactionCategoryMapper domainMapper;

  @Override
  public List<TransactionCategory> findByIdAccountAndType(
      String idAccount,
      TransactionTypeEnum type,
      LocalDate startDate, LocalDate endDate) {
    List<HTransactionCategory> entities =
        findAllByCriteria(idAccount, type);
    if (entities.isEmpty()) {
      return List.of();
    }
    return entities.stream()
        .map(entity -> domainMapper.toDomain(idAccount, entity, startDate, endDate))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<TransactionCategory> saveAll(List<TransactionCategory> toCreate) {
    List<HTransactionCategory> entitiesToCreate = toCreate.stream()
        .map(domainMapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entitiesToCreate).stream()
        .map(category -> domainMapper.toDomain(category, DEFAULT_START_DATE, LocalDate.now()))
        .collect(Collectors.toUnmodifiableList());
  }

  @Transactional
  @Override
  public TransactionCategory findByIdTransaction(String idTransaction) {
    HTransactionCategory persisted =
        jpaRepository.findTopByIdTransactionOrderByCreatedDatetimeDesc(idTransaction);
    if (persisted == null) {
      return null;
    }
    return domainMapper.toDomain(persisted, DEFAULT_START_DATE, LocalDate.now());
  }


  public List<HTransactionCategory> findAllByCriteria(
      String idAccount, TransactionTypeEnum type) {
    if (type == null) {
      return jpaRepository.findAllByIdAccount(idAccount);
    }
    return jpaRepository.findAllByIdAccountAndType(idAccount, type);
  }
}
