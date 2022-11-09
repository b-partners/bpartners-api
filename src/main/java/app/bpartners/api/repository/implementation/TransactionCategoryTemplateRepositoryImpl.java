package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.TransactionCategoryTemplate;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.TransactionCategoryMapper;
import app.bpartners.api.repository.TransactionCategoryTemplateRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryTemplateJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionCategoryTemplateRepositoryImpl implements
    TransactionCategoryTemplateRepository {

  private final TransactionCategoryTemplateJpaRepository jpaRepository;
  private final TransactionCategoryMapper mapper;

  @Override
  public TransactionCategoryTemplate findByType(String type) {
    return mapper.toDomain(
        jpaRepository.findByType(type)
            .orElseThrow(
                () -> new NotFoundException("Transaction category " + type + " not found. "
                    + "Creation of a new one is not supported yet."))
    );
  }
}
