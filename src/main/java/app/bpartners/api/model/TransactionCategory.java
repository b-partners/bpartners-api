package app.bpartners.api.model;

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
  private String idTransaction;
  private String idTransactionCategoryTmpl;
  private String type;
  private Long typeCount;
  private String comment;
}