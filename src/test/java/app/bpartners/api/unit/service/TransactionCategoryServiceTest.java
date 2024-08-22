package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.TransactionCategoryTemplateRepository;
import app.bpartners.api.service.TransactionCategoryService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TransactionCategoryServiceTest {
  TransactionCategoryService service;
  TransactionCategoryRepository repository;
  TransactionCategoryTemplateRepository templateRepository;

  @BeforeEach
  public void setUp() throws Exception {
    repository = mock(TransactionCategoryRepository.class);
    templateRepository = mock(TransactionCategoryTemplateRepository.class);
    service = new TransactionCategoryService(repository, templateRepository);
  }

  @Test
  public void getCategoriesByAccountType_ok() {
    var transactionCategory1 = new TransactionCategory();
    var transactionCategory2 = new TransactionCategory();

    when(repository.findByIdAccountAndType(any(), any(), any(), any()))
        .thenReturn(List.of(transactionCategory1, transactionCategory2));

    var actual =
        service.getCategoriesByAccountAndType(
            "", TransactionTypeEnum.INCOME, LocalDate.now(), LocalDate.now());
    assertTrue(actual.contains(transactionCategory1));
    assertTrue(actual.contains(transactionCategory2));
  }
}
