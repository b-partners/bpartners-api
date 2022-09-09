package app.bpartners.api.service;

import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.TransactionCategoryType;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.TransactionRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static app.bpartners.api.conf.TransactionConf.OTHER_CATEGORY_ID;

@Service
@AllArgsConstructor
public class TransactionService {
  private final TransactionRepository repository;
  private final TransactionCategoryService categoryService;
  private final TransactionCategoryTypeService categoryTypeService;

  public List<Transaction> getTransactionsByAccountId(String accountId) {
    return repository.findByAccountId(accountId);
  }

  public Transaction getTransactionById(String id) {
    return repository.findById(id);
  }

  public Transaction modifyTransaction(
      String transactionId,
      Transaction toChange) {
    TransactionCategory transactionCategory =
        categoryService.getTransactionCategoryByIdTransaction(transactionId);
    TransactionCategoryType type =
        categoryTypeService.getCategoryById(toChange.getCategory().getType().getId());
    String comment = null;
    //TODO : To put in validator
    if (!type.getId().equals(OTHER_CATEGORY_ID) && toChange.getCategory().getComment() != null) {
      throw new BadRequestException("Comment is not allowed for category [" + type.getLabel()
          + "]");
    }
    if (type.getId().equals(OTHER_CATEGORY_ID)) {
      comment = toChange.getCategory().getComment();
      if (comment == null) {
        throw new BadRequestException("Comment is mandatory for the category type \"Other\"");
      }
    }

    // END OF TODO
    if (transactionCategory != null) {
      transactionCategory.setType(type);
      transactionCategory.setComment(comment);
    } else {
      transactionCategory =
          new TransactionCategory(null, comment, transactionId,
              type);
    }
    categoryService.saveTransactionCategory(transactionId, transactionCategory);
    return getTransactionById(transactionId);
  }
}
