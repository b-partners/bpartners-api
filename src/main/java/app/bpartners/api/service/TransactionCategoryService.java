package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.TransactionCategoryTemplate;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.TransactionCategoryTemplateRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionCategoryService {
  private final TransactionCategoryRepository repository;
  private final TransactionCategoryTemplateRepository templateRepository;

  public List<TransactionCategory> getCategoriesByAccountAndType(
      String idAccount, TransactionTypeEnum type, LocalDate startDate, LocalDate endDate) {
    return repository.findByIdAccountAndType(idAccount, type, startDate, endDate);
  }

  public List<TransactionCategory> createCategories(List<TransactionCategory> toCreate) {
    return repository.saveAll(toCreate);
  }

  public List<TransactionCategoryTemplate> getCategoryTemplates(
      String idAccount, TransactionTypeEnum type, LocalDate begin, LocalDate end) {
    if (type == null) {
      return templateRepository.findAllByIdAccount(idAccount, begin, end);
    }
    return templateRepository.findAllByIdAccountAndType(idAccount, type, begin, end);
  }
}
