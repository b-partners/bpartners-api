package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.Invoice;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL_CONFIRMED;
import static app.bpartners.api.endpoint.rest.security.AuthProvider.userIsAuthenticated;
import static app.bpartners.api.service.InvoiceService.DRAFT_TEMPLATE;
import static app.bpartners.api.service.InvoiceService.INVOICE_TEMPLATE;
import static java.util.UUID.randomUUID;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Repository
@AllArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {
  private final InvoiceJpaRepository jpaRepository;
  private final PaymentRequestJpaRepository paymentReqJpaRepository;
  private final InvoiceMapper mapper;
  private final InvoiceProductMapper productMapper;
  private final InvoiceProductJpaRepository productJpaRepository;
  private final FileService fileService;
  private final InvoicePdfUtils pdfUtils = new InvoicePdfUtils();

  @Override
  public Invoice crupdate(Invoice actual) {
    List<HPaymentRequest> paymentRequests = actual.getPaymentRegulations().stream()
        .map(paymentRegulation -> new HPaymentRequest(paymentRegulation.getPaymentRequest()))
        .collect(Collectors.toList());
    List<HInvoiceProduct> invoiceProducts = actual.getProducts().stream()
        .map(productMapper::toEntity)
        .collect(Collectors.toUnmodifiableList());

    Invoice toBeSaved = actual;
    if (!paymentRequests.isEmpty()
        && actual.getStatus() != CONFIRMED && actual.getStatus() != PAID) {
      paymentReqJpaRepository.deleteAllByIdInvoice(toBeSaved.getId());
    }
    if (!invoiceProducts.isEmpty()) {
      productJpaRepository.deleteAllByIdInvoice(toBeSaved.getId());
    }
    if (actual.getStatus() == PROPOSAL_CONFIRMED) {
      HInvoice proposalConfirmedInvoice = mapper.toEntity(actual, paymentRequests, invoiceProducts);
      jpaRepository.save(proposalConfirmedInvoice);

      toBeSaved = actual.toBuilder()
          .id(String.valueOf(randomUUID()))
          .status(CONFIRMED)
          .sendingDate(LocalDate.now())
          .validityDate(null)
          .fileId(null) //To be generated later from processPdfGeneration
          .build();
    }
    HInvoice actualEntity = mapper.toEntity(toBeSaved, paymentRequests, invoiceProducts);
    Invoice toGenerateAsPdf =
        mapper.toDomain(
            actualEntity
                .products(invoiceProducts)
                .paymentRequests(paymentRequests), AuthProvider.getAuthenticatedUser());
    HInvoice toSave = actualEntity.fileId(processAsPdf(toGenerateAsPdf));
    HInvoice savedInvoice = jpaRepository.save(toSave);

    return mapper.toDomain(savedInvoice, toGenerateAsPdf.getUser());
  }

  public String processAsPdf(Invoice domain) {
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
  public Optional<Invoice> pwFindOptionalById(String id) {
    Optional<HInvoice> optional = jpaRepository.findOptionalById(id);
    return optional.map(mapper::toDomain);
  }

  @Override
  public List<Invoice> findAllByIdUserAndStatusesAndArchiveStatus(String idUser,
                                                                  List<InvoiceStatus> statusList,
                                                                  ArchiveStatus archiveStatus,
                                                                  String title,
                                                                  int page,
                                                                  int pageSize) {
    PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(DESC, "createdDatetime"));
    return jpaRepository.findAllByIdUserAndArchiveStatusAndTitleContainingIgnoreCaseAndStatusIn(
            idUser,
            archiveStatus,
            title,
            statusList,
            pageRequest).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Invoice> findAllByIdUserAndArchiveStatus(String idUser,
                                                       ArchiveStatus archiveStatus,
                                                       String title,
                                                       int page,
                                                       int pageSize) {
    PageRequest pageable = PageRequest.of(page, pageSize, Sort.by(DESC, "createdDatetime"));
    return jpaRepository.findAllByIdUserAndArchiveStatusAndTitleContainingIgnoreCase(
            idUser,
            archiveStatus,
            title,
            pageable).stream()
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

  @Override
  public List<Invoice> findByIdUserAndRef(String idUser, String reference) {
    return jpaRepository.findByIdUserAndRef(idUser, reference).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  private String userLogoFileId() {
    return userIsAuthenticated()
        ? AuthProvider.getAuthenticatedUser().getLogoFileId()
        : null;
  }
}
