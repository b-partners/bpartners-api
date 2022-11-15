package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class TransactionCategory {
  private String id;
  private String idAccount;
  private String idTransaction;
  private String idTransactionCategoryTmpl;
  private String type;
  private long typeCount;
  private String comment;
  private String description;
  private TransactionTypeEnum transactionType;
}