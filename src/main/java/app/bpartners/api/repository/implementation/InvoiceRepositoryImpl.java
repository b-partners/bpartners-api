package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.AccountHolder;
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
import app.bpartners.api.service.AccountHolderService;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.utils.InvoicePdfUtils;
import java.util.ArrayList;
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
import static app.bpartners.api.service.InvoiceService.DRAFT_TEMPLATE;
import static app.bpartners.api.service.InvoiceService.INVOICE_TEMPLATE;
import static java.util.UUID.randomUUID;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Repository
@AllArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {
  private final InvoiceJpaRepository jpaRepository;
  private final PaymentRequestJpaRepository requestJpaRepository;
  private final PrincipalProvider auth;
  private final InvoiceMapper mapper;
  private final InvoiceProductMapper productMapper;
  private final InvoiceProductJpaRepository productJpaRepository;
  private final AccountHolderService holderService;
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
    HInvoice persistedEntity = jpaRepository.save(entity
        .fileId(processPdfGeneration(mapper.toDomain(entityWithProdAndPay))));
    return mapper.toDomain(persistedEntity);
  }

  private String processPdfGeneration(Invoice domain) {
    String fileId = domain.getFileId() == null
        ? String.valueOf(randomUUID()) : domain.getFileId();
    String accountId = domain.getAccount().getId();

    List<byte[]> logos = fileService.downloadOptionalFile(LOGO, accountId, userLogoFileId());
    byte[] logoAsBytes = logos.isEmpty() ? null : logos.get(0);
    byte[] fileAsBytes = domain.getStatus() == CONFIRMED || domain.getStatus() == PAID
        ? pdfUtils.generatePdf(domain, accountHolder(domain), logoAsBytes, INVOICE_TEMPLATE)
        : pdfUtils.generatePdf(domain, accountHolder(domain), logoAsBytes, DRAFT_TEMPLATE);
    String id = fileService.upload(fileId, INVOICE, accountId, fileAsBytes, null).getId();
    domain.setFileId(id);

    return id;
  }

  @Override
  public Invoice getById(String invoiceId) {
    Optional<HInvoice> optionalInvoice = jpaRepository.findById(invoiceId);
    if (optionalInvoice.isEmpty()) {
      throw new NotFoundException("Invoice." + invoiceId + " is not found");
    }
    HInvoice invoice = optionalInvoice.get();
    return mapper.toDomain(invoice);
  }

  @Override
  public List<Invoice> findAllByAccountIdAndStatus(
      String accountId, InvoiceStatus status, int page, int pageSize) {
    PageRequest pageRequest = PageRequest.of(page, pageSize, Sort.by(DESC, "createdDatetime"));
    return jpaRepository.findAllByIdAccountAndStatus(accountId, status, pageRequest).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Invoice> findAllByAccountId(String accountId, int page, int pageSize) {
    PageRequest pageable = PageRequest.of(page, pageSize, Sort.by(DESC, "createdDatetime"));
    return jpaRepository.findAllByIdAccount(accountId, pageable).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  private List<HInvoiceProduct> getProductEntities(Invoice toCrupdate, HInvoice invoice) {
    return toCrupdate.getProducts().stream()
        .map(product -> productMapper.toEntity(product, invoice))
        .collect(Collectors.toUnmodifiableList());
  }

  private String userLogoFileId() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getUser().getLogoFileId();
  }

  private AccountHolder accountHolder(Invoice toCrupdate) {
    return holderService.getAccountHolderByAccountId(toCrupdate.getAccount().getId());
  }
}
