package app.bpartners.api.service;

import app.bpartners.api.model.TransactionCategoryType;
import app.bpartners.api.repository.TransactionCategoryTypeRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class TransactionCategoryTypeService {
  private final TransactionCategoryTypeRepository repository;

  public List<TransactionCategoryType> getCategoryTypes() {
    return repository.findAll();
  }

  public List<TransactionCategoryType> createCategoryTypes(List<TransactionCategoryType> toCreate) {
    return repository.saveAll(toCreate);
  }

  public TransactionCategoryType getCategoryById(String id) {
    return repository.findById(id);
  }
}
