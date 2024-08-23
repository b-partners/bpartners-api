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

class TransactionCategoryServiceTest {
  TransactionCategoryService serviceMock;
  TransactionCategoryRepository repositoryMock;
  TransactionCategoryTemplateRepository templateRepositoryMock;

  @BeforeEach
  void setUp() {
    repositoryMock = mock(TransactionCategoryRepository.class);
    templateRepositoryMock = mock(TransactionCategoryTemplateRepository.class);
    serviceMock = new TransactionCategoryService(repositoryMock, templateRepositoryMock);
  }

  @Test
  void getCategoriesByAccountTypeOk() {
    var transactionCategory1 = new TransactionCategory();
    var transactionCategory2 = new TransactionCategory();

    when(repositoryMock.findByIdAccountAndType(any(), any(), any(), any()))
        .thenReturn(List.of(transactionCategory1, transactionCategory2));

    var actual =
        serviceMock.getCategoriesByAccountAndType(
            "", TransactionTypeEnum.INCOME, LocalDate.now(), LocalDate.now());
    assertTrue(actual.contains(transactionCategory1));
    assertTrue(actual.contains(transactionCategory2));
  }
}
