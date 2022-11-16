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
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionCategoryRepositoryImpl implements TransactionCategoryRepository {
  private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2022, 1, 1);
  private final TransactionCategoryJpaRepository jpaRepository;
  private final TransactionCategoryMapper mapper;

  @Override
  public List<TransactionCategory> findAllByIdAccountAndType(
      String idAccount,
      TransactionTypeEnum type,
      LocalDate startDate, LocalDate endDate) {
    List<HTransactionCategory> categories;
    if (type == null) {
      categories = jpaRepository.findAllByIdAccount(idAccount);
    } else {
      categories = jpaRepository.findAllByIdAccountAndType(idAccount, type);
    }

    return categories
        .stream()
        .map(category -> mapper.toDomain(category, startDate, endDate))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<TransactionCategory> saveAll(List<TransactionCategory> toCreate) {
    List<HTransactionCategory> entitiesToCreate = toCreate.stream()
        .map(mapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entitiesToCreate).stream()
        .map(category -> mapper.toDomain(category, DEFAULT_START_DATE, LocalDate.now()))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public TransactionCategory findByIdTransaction(String idTransaction) {
    HTransactionCategory entity =
        jpaRepository.findTopByIdTransactionOrderByCreatedDatetimeDesc(idTransaction);
    return mapper.toDomain(entity, DEFAULT_START_DATE, LocalDate.now());
  }
}
