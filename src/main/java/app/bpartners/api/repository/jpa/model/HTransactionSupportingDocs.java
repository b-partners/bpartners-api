package app.bpartners.api.repository.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "\"transaction_supporting_docs\"")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
public class HTransactionSupportingDocs {
  @Id private String id;

  @Column(name = "id_transaction")
  private String idTransaction;

  @ManyToOne
  @JoinColumn(name = "id_file_info")
  private HFileInfo fileInfo;
}
