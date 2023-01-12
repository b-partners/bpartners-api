package app.bpartners.api.repository.jpa.model;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.repository.jpa.types.PostgresEnumType;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;

@Entity
@Table(name = "\"invoice\"")
@TypeDef(name = "pgsql_enum", typeClass = PostgresEnumType.class)
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HInvoice implements Serializable {
  @Id
  private String id;
  @Column(name = "\"ref\"")
  private String ref;
  private String title;
  private String idAccount;
  private String paymentUrl;
  private LocalDate sendingDate;
  private LocalDate validityDate;
  private Integer delayInPaymentAllowed;
  private String delayPenaltyPercent;
  private LocalDate toPayAt;
  private String comment;
  private String fileId;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private InvoiceStatus status;
  @ManyToOne
  @JoinColumn(name = "id_customer")
  private HCustomer customer;
  private String customerEmail;
  private String customerPhone;
  private String customerAddress;
  private String customerWebsite;
  private String customerCity;
  private Integer customerZipCode;
  private String customerCountry;
  @OneToMany(
      mappedBy = "invoice",
      orphanRemoval = true,
      cascade = CascadeType.ALL
  )
  private List<HInvoiceProduct> products = new ArrayList<>();
  @CreationTimestamp
  @Column(updatable = false)
  private Instant createdDatetime;
  private Instant updatedAt;
  private boolean toBeRelaunched;
  private String metadataString;
}
