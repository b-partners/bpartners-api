package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.mapper.TransactionCategoryMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionCategoryRepositoryImpl implements TransactionCategoryRepository {
  private final TransactionCategoryJpaRepository jpaRepository;
  private final TransactionCategoryMapper mapper;

  @Override
  public TransactionCategory findByIdTransaction(String idTransaction) {
    Optional<HTransactionCategory> entity =
        jpaRepository.findHTransactionCategoryByIdTransaction(idTransaction);
    if (entity.isEmpty()) {
      return null;
    }
    return mapper.toDomain(entity.get());
  }

  @Override
  public TransactionCategory save(String transactionId, TransactionCategory toCreate) {
    //TODO : Check if transaction exists upper
    HTransactionCategory entity = mapper.toEntity(toCreate);
    entity.setIdTransaction(transactionId);
    return mapper.toDomain(jpaRepository.save(entity));
  }
}
