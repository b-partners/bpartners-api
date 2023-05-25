package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.InvoiceProductMapper;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceProductJpaRepository;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.utils.InvoicePdfUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.security.AuthProvider.userIsAuthenticated;
import static app.bpartners.api.service.InvoiceService.DRAFT_TEMPLATE;
import static app.bpartners.api.service.InvoiceService.INVOICE_TEMPLATE;
import static java.util.UUID.randomUUID;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Repository
@AllArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {
  private final InvoiceJpaRepository jpaRepository;
  private final PaymentRequestJpaRepository requestJpaRepository;
  private final InvoiceMapper mapper;
  private final InvoiceProductMapper productMapper;
  private final InvoiceProductJpaRepository productJpaRepository;
  private final FileService fileService;
  private final InvoicePdfUtils pdfUtils = new InvoicePdfUtils();

  @Override
  public Invoice crupdate(Invoice invoice) {
    HInvoice entity = mapper.toEntity(invoice, true);
    List<HPaymentRequest> paymentRequests = new ArrayList<>(entity.getPaymentRequests());
    if (!entity.getProducts().isEmpty()) {
      productJpaRepository.deleteAll(entity.getProducts());
    }
    if (!entity.getPaymentRequests().isEmpty()
        && invoice.getStatus() != CONFIRMED && invoice.getStatus() != PAID) {
      requestJpaRepository.deleteAllByIdInvoice(entity.getId());
    }
    HInvoice entityWithProdAndPay = entity
        .products(getProductEntities(invoice, entity))
        .paymentRequests(paymentRequests);
    User authenticatedUser = AuthProvider.getAuthenticatedUser();
    HInvoice persistedEntity = jpaRepository.save(entity
        .fileId(processPdfGeneration(
            mapper.toDomain(entityWithProdAndPay, authenticatedUser))));
    return mapper.toDomain(persistedEntity, authenticatedUser);
  }

  private String processPdfGeneration(Invoice domain) {
    String fileId = domain.getFileId() == null
        ? String.valueOf(randomUUID()) : domain.getFileId();
    String idUser = domain.getUser().getId();

    List<byte[]> logos = fileService.downloadOptionalFile(LOGO, idUser, userLogoFileId());
    byte[] logoAsBytes = logos.isEmpty() ? null : logos.get(0);
    byte[] fileAsBytes = domain.getStatus() == CONFIRMED || domain.getStatus() == PAID
        ? pdfUtils.generatePdf(domain, domain.getActualHolder(), logoAsBytes, INVOICE_TEMPLATE)
        : pdfUtils.generatePdf(domain, domain.getActualHolder(), logoAsBytes, DRAFT_TEMPLATE);
    String id = fileService.upload(fileId, INVOICE, idUser, fileAsBytes).getId(); //TODO
    domain.setFileId(id);

    return id;
  }

  @Override
  public Invoice getById(String invoiceId) {
    return mapper.toDomain(jpaRepository.findById(invoiceId).orElseThrow(
            () -> new NotFoundException("Invoice." + invoiceId + " is not found")),
        AuthProvider.getAuthenticatedUser());
  }

  @Override
  public List<Invoice> findAllByIdUserAndStatus(
      String idUser, InvoiceStatus status, int page, int pageSize) {
    PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(DESC, "createdDatetime"));
    return jpaRepository.findAllByIdUserAndStatus(idUser, status, pageRequest).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Invoice> findAllByIdUser(String idUser, int page, int pageSize) {
    PageRequest pageable = PageRequest.of(page, pageSize, Sort.by(DESC, "createdDatetime"));
    return jpaRepository.findAllByIdUser(idUser, pageable).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Invoice> saveAll(List<ArchiveInvoice> archiveInvoices) {
    List<HInvoice> entities = archiveInvoices.stream()
        .map(archiveInvoice -> {
          HInvoice invoice = jpaRepository.findById(archiveInvoice.getIdInvoice())
              .orElseThrow(() -> new NotFoundException(
                  "Invoice(id=" + archiveInvoice.getIdInvoice() + " not found"));
          return invoice.toBuilder()
              .archiveStatus(archiveInvoice.getStatus())
              .build();
        })
        .collect(Collectors.toList());
    return jpaRepository.saveAll(entities).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  private List<HInvoiceProduct> getProductEntities(Invoice toCrupdate, HInvoice invoice) {
    return toCrupdate.getProducts().stream()
        .map(product -> productMapper.toEntity(product, invoice))
        .collect(Collectors.toUnmodifiableList());
  }

  private String userLogoFileId() {
    return userIsAuthenticated()
        ? AuthProvider.getAuthenticatedUser().getLogoFileId()
        : null;
  }
}
