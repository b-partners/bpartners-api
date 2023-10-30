package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.TransactionCategoryTemplate;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.TransactionCategoryMapper;
import app.bpartners.api.repository.TransactionCategoryTemplateRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryTemplateJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategoryTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionCategoryTemplateRepositoryImpl implements
    TransactionCategoryTemplateRepository {

  private final TransactionCategoryTemplateJpaRepository jpaRepository;
  private final TransactionCategoryMapper mapper;

  @Override
  public List<TransactionCategoryTemplate> findByType(String type) {
    List<HTransactionCategoryTemplate> categories = jpaRepository.findAllByType(type);
    if (categories.isEmpty()) {
      throw new NotFoundException("Category " + type + " is not found");
    }
    return categories.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public TransactionCategoryTemplate findByTypeAndTransactionType(
      String type,
      TransactionTypeEnum transactionType) {
    return mapper.toDomain(jpaRepository.findByTypeAndTransactionType(type, transactionType));
  }

  @Override
  public List<TransactionCategoryTemplate> findAllByIdAccount(
      String idAccount, LocalDate begin, LocalDate end) {
    return jpaRepository.findAllByIdAccount(idAccount, begin, end).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<TransactionCategoryTemplate> findAllByIdAccountAndType(
      String idAccount, TransactionTypeEnum transactionType, LocalDate begin, LocalDate end) {
    return jpaRepository.findAllByIdAccountAndType(
            transactionType.getValue(), idAccount, begin, end).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
