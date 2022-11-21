package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class TransactionCategory {
  private String id;
  private String idAccount;
  private String idTransactionCategoryTmpl;
  private String type;
  private Fraction vat;
  private String idTransaction;
  private TransactionTypeEnum transactionType;
  private Long typeCount;

  public boolean isUserDefined() {
    return idTransactionCategoryTmpl == null;
  }
}
