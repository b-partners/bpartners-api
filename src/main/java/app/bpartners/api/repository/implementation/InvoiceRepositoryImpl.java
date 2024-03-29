package app.bpartners.api.repository.implementation;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL_CONFIRMED;
import static app.bpartners.api.service.InvoiceService.DRAFT_TEMPLATE;
import static app.bpartners.api.service.InvoiceService.INVOICE_TEMPLATE;
import static java.util.UUID.randomUUID;
import static org.springframework.data.domain.Sort.Direction.DESC;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.InvoiceProductMapper;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceProductJpaRepository;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.utils.InvoicePdfUtils;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {
  private final InvoiceJpaRepository jpaRepository;
  private final PaymentRequestJpaRepository paymentReqJpaRepository;
  private final InvoiceMapper mapper;
  private final InvoiceProductMapper productMapper;
  private final InvoiceProductJpaRepository productJpaRepository;
  private final FileService fileService;
  private final EntityManager entityManager;
  protected final UserRepository userRepository;
  private final InvoicePdfUtils pdfUtils = new InvoicePdfUtils();

  @Override
  public List<Invoice> findAllEnabledByIdUser(String idUser) {
    return jpaRepository.findAllByIdUserAndArchiveStatus(idUser, ArchiveStatus.ENABLED).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Invoice crupdate(Invoice actual) {
    List<HPaymentRequest> paymentRequests =
        actual.getPaymentRegulations().stream()
            .map(paymentRegulation -> new HPaymentRequest(paymentRegulation.getPaymentRequest()))
            .collect(Collectors.toList());
    List<HInvoiceProduct> invoiceProducts =
        actual.getProducts().stream().map(productMapper::toEntity).toList();

    Invoice toBeSaved = actual;
    if (!paymentRequests.isEmpty()
        && actual.getStatus() != CONFIRMED
        && actual.getStatus() != PAID) {
      paymentReqJpaRepository.deleteAllByIdInvoice(toBeSaved.getId());
    }
    if (!invoiceProducts.isEmpty()) {
      productJpaRepository.deleteAllByIdInvoice(toBeSaved.getId());
    }
    if (actual.getStatus() == PROPOSAL_CONFIRMED) {
      HInvoice proposalConfirmedInvoice = mapper.toEntity(actual, paymentRequests, invoiceProducts);
      jpaRepository.save(proposalConfirmedInvoice);

      toBeSaved =
          actual.toBuilder()
              .id(String.valueOf(randomUUID()))
              .status(CONFIRMED)
              .sendingDate(LocalDate.now())
              .validityDate(null)
              .fileId(null) // To be generated later from processPdfGeneration
              .build();
    }
    HInvoice actualEntity = mapper.toEntity(toBeSaved, paymentRequests, invoiceProducts);
    Invoice toGenerateAsPdf =
        mapper.toDomain(
            actualEntity.products(invoiceProducts).paymentRequests(paymentRequests),
            actual.getUser());
    HInvoice toSave = actualEntity.fileId(processAsPdf(toGenerateAsPdf));
    HInvoice savedInvoice = jpaRepository.save(toSave);

    return mapper.toDomain(savedInvoice, toGenerateAsPdf.getUser());
  }

  public String processAsPdf(Invoice domain) {
    String fileId = domain.getFileId() == null ? String.valueOf(randomUUID()) : domain.getFileId();
    String idUser = domain.getUser().getId();

    List<byte[]> logos =
        fileService.downloadOptionalFile(LOGO, idUser, domain.getUser().getLogoFileId());
    byte[] logoAsBytes = logos.isEmpty() ? null : logos.get(0);
    byte[] fileAsBytes =
        domain.getStatus() == CONFIRMED || domain.getStatus() == PAID
            ? pdfUtils.generatePdf(domain, domain.getActualHolder(), logoAsBytes, INVOICE_TEMPLATE)
            : pdfUtils.generatePdf(domain, domain.getActualHolder(), logoAsBytes, DRAFT_TEMPLATE);
    String id = fileService.upload(fileId, INVOICE, idUser, fileAsBytes).getId(); // TODO
    domain.setFileId(id);

    return id;
  }

  @Override
  public Invoice getById(String invoiceId) {
    HInvoice invoice =
        jpaRepository
            .findById(invoiceId)
            .orElseThrow(() -> new NotFoundException("Invoice." + invoiceId + " is not found"));
    return mapper.toDomain(invoice, userRepository.getById(invoice.getIdUser()));
  }

  @Override
  public Optional<Invoice> pwFindOptionalById(String id) {
    Optional<HInvoice> optional = jpaRepository.findOptionalById(id);
    return optional.map(mapper::toDomain);
  }

  @Override
  public List<Invoice> findAllByIdUserAndStatusesAndArchiveStatus(
      String idUser,
      List<InvoiceStatus> statusList,
      ArchiveStatus archiveStatus,
      String title,
      int page,
      int pageSize) {
    PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(DESC, "createdDatetime"));
    return jpaRepository
        .findAllByIdUserAndArchiveStatusAndTitleContainingIgnoreCaseAndStatusIn(
            idUser, archiveStatus, title, statusList, pageRequest)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Invoice> findAllByIdUserAndCriteria(
      String idUser,
      List<InvoiceStatus> statusList,
      ArchiveStatus archiveStatus,
      List<String> filters,
      int page,
      int pageSize) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HInvoice> query = builder.createQuery(HInvoice.class);
    List<Predicate> predicates = new ArrayList<>();
    Root<HInvoice> root = query.from(HInvoice.class);

    predicates.add(builder.equal(root.get("idUser"), idUser));
    predicates.add(builder.equal(root.get("archiveStatus"), archiveStatus));
    if (statusList != null) {
      predicates.add(
          builder.or(
              statusList.stream()
                  .map(status -> builder.equal(root.get("status"), status))
                  .toArray(Predicate[]::new)));
    }
    if (!filters.isEmpty()) {
      List<Predicate> filtersPredicates = new ArrayList<>();
      for (String filter : filters) {
        filtersPredicates.add(
            builder.like(builder.lower(root.get("title")), "%" + filter.toLowerCase() + "%"));
        filtersPredicates.add(
            builder.like(builder.lower(root.get("ref")), "%" + filter.toLowerCase() + "%"));
        setCustomerFilters(builder, root, filtersPredicates, filter);
      }
      predicates.add(builder.or(filtersPredicates.toArray(new Predicate[0])));
    }
    Pageable pageable = PageRequest.of(page, pageSize, Sort.by(DESC, "createdDatetime"));
    query
        .where(builder.and(predicates.toArray(new Predicate[0])))
        .orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder));
    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList()
        .stream()
        .map(invoice -> mapper.toDomain(invoice, userRepository.getById(idUser)))
        .toList();
  }

  private void setCustomerFilters(
      CriteriaBuilder builder,
      Root<HInvoice> rootPath,
      List<Predicate> filtersPredicates,
      String filter) {
    Path<String> customerPath = rootPath.get("customer");
    filtersPredicates.add(
        builder.like(
            builder.lower(customerPath.get("firstName")), "%" + filter.toLowerCase() + "%"));
    filtersPredicates.add(
        builder.like(
            builder.lower(customerPath.get("lastName")), "%" + filter.toLowerCase() + "%"));
    filtersPredicates.add(
        builder.like(builder.lower(customerPath.get("email")), "%" + filter.toLowerCase() + "%"));
    filtersPredicates.add(
        builder.like(builder.lower(customerPath.get("phone")), "%" + filter.toLowerCase() + "%"));
    filtersPredicates.add(
        builder.like(builder.lower(customerPath.get("address")), "%" + filter.toLowerCase() + "%"));
    filtersPredicates.add(
        builder.like(builder.lower(customerPath.get("city")), "%" + filter.toLowerCase() + "%"));
    filtersPredicates.add(
        builder.like(builder.lower(customerPath.get("country")), "%" + filter.toLowerCase() + "%"));
  }

  @Override
  public List<Invoice> findAllByIdUserAndArchiveStatus(
      String idUser, ArchiveStatus archiveStatus, String title, int page, int pageSize) {
    PageRequest pageable = PageRequest.of(page, pageSize, Sort.by(DESC, "createdDatetime"));
    return jpaRepository
        .findAllByIdUserAndArchiveStatusAndTitleContainingIgnoreCase(
            idUser, archiveStatus, title, pageable)
        .stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Invoice> saveAll(List<ArchiveInvoice> archiveInvoices) {
    List<HInvoice> entities =
        archiveInvoices.stream()
            .map(
                archiveInvoice -> {
                  HInvoice invoice =
                      jpaRepository
                          .findById(archiveInvoice.getIdInvoice())
                          .orElseThrow(
                              () ->
                                  new NotFoundException(
                                      "Invoice(id="
                                          + archiveInvoice.getIdInvoice()
                                          + " not found"));
                  return invoice.toBuilder().archiveStatus(archiveInvoice.getStatus()).build();
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

  @Override
  public Invoice findById(String id) {
    HInvoice invoice = jpaRepository.getById(id);
    User user = userRepository.getById(invoice.getIdUser());
    return mapper.toDomain(invoice, user);
  }
}
