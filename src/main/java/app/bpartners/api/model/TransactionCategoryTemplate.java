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
public class TransactionCategoryTemplate {
  private String id;
  private String type;
  private TransactionTypeEnum transactionType;
  private Fraction vat;
  private boolean other;
  private String description;
}