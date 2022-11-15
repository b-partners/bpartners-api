package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.mapper.TransactionCategoryMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryJpaRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryTemplateJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import app.bpartners.api.repository.jpa.model.HTransactionCategoryTemplate;
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
  private final TransactionCategoryTemplateJpaRepository templateJpaRepository;
  private final TransactionCategoryMapper mapper;

  @Override
  public List<TransactionCategory> findAllByIdAccount(
      String idAccount,
      LocalDate startDate, LocalDate endDate) {
    List<HTransactionCategoryTemplate> templates = templateJpaRepository.findAll();
    //TODO:
    //Expected should be at least 12 because all the categories template should be sent
    //Others categories templates should also be returned with their comment values
    return jpaRepository.findAllByIdAccount(idAccount).stream()
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
