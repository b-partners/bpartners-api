package app.bpartners.api.repository.jpa.model;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "\"invoice_relaunch_conf\"")
public class HInvoiceRelaunchConf {
  @Id
  @GeneratedValue(strategy = IDENTITY)
  private String id;

  private String idInvoice;
  private int delay;
  private int rehearsalNumber;
}
