package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
