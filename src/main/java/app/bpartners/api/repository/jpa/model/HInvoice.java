package app.bpartners.api.repository.jpa.model;

import static jakarta.persistence.FetchType.EAGER;
import static org.hibernate.type.SqlTypes.NAMED_ENUM;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "\"invoice\"")
@Getter
@Setter
@ToString
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
public class HInvoice implements Serializable {
  @Id private String id;

  @Column(name = "\"ref\"")
  private String ref;

  private String title;
  private String idUser;
  private String paymentUrl;
  private LocalDate sendingDate;
  private LocalDate validityDate;
  private Integer delayInPaymentAllowed;
  private String delayPenaltyPercent;
  private LocalDate toPayAt;
  private String comment;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private PaymentTypeEnum paymentType;

  private String fileId;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private InvoiceStatus status;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private ArchiveStatus archiveStatus;

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

  @OneToMany(orphanRemoval = true, cascade = CascadeType.ALL, fetch = EAGER)
  @JoinColumn(name = "id_invoice")
  private List<HInvoiceProduct> products = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = EAGER)
  @JoinColumn(name = "id_invoice")
  private List<HPaymentRequest> paymentRequests;

  @CreationTimestamp
  @Column(updatable = false)
  private Instant createdDatetime;

  private Instant updatedAt;
  private boolean toBeRelaunched;
  private String metadataString;
  private String discountPercent;
  private String idAreaPicture;

  @JdbcTypeCode(NAMED_ENUM)
  @Enumerated(EnumType.STRING)
  private PaymentMethod paymentMethod;

  public HInvoice products(List<HInvoiceProduct> products) {
    this.products = products;
    return this;
  }

  public HInvoice paymentRequests(List<HPaymentRequest> paymentRequests) {
    this.paymentRequests = paymentRequests;
    return this;
  }

  public HInvoice fileId(String fileId) {
    this.fileId = fileId;
    return this;
  }

  public HInvoice status(InvoiceStatus status) {
    this.status = status;
    return this;
  }

  public String describe() {
    return "Invoice(id=" + id + ",reference=" + ref + ",user=" + idUser + ")";
  }
}
