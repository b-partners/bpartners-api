package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.TransactionCategoryTemplate;
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
  public TransactionCategoryTemplate findByTypeAndVat(String type, Fraction vat) {
    return mapper.toDomain(jpaRepository.findByTypeAndVat(type, vat.toString()));
  }
}
