package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.InvoiceProductMapper;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import app.bpartners.api.service.AccountHolderService;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.utils.InvoicePdfUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Repository
@AllArgsConstructor
public class InvoiceRepositoryImpl implements InvoiceRepository {
  private final InvoiceJpaRepository jpaRepository;
  private final PrincipalProvider auth;
  private final InvoiceMapper mapper;
  private final InvoiceProductMapper productMapper;
  private final InvoiceProductJpaRepository productJpaRepository;
  private final AccountHolderService holderService;
  private final FileService fileService;
  private final InvoicePdfUtils pdfUtils = new InvoicePdfUtils();

  @Override
  public Invoice crupdate(Invoice toCrupdate) {
    //TODO: check case when ref is still null
    //As ref is nullable, we might have a list of invoice with null ref
    Optional<List<HInvoice>> optionalInvoice = jpaRepository.findByIdAccountAndRefAndStatus(
        toCrupdate.getAccount().getId(), toCrupdate.getRealReference(), toCrupdate.getStatus());
    if (optionalInvoice.isPresent()) {
      List<HInvoice> existingInvoiceList = optionalInvoice.get();
      if (!existingInvoiceList.isEmpty()) {
        HInvoice invoice = existingInvoiceList.get(0);
        if (toCrupdate.getRef() != null
            && !toCrupdate.getId().equals(invoice.getId())) {
          throw new BadRequestException(
              "The invoice reference must be unique however the given reference ["
                  + toCrupdate.getRef()
                  + "] is already used by invoice." + invoice.getId());
        }
      }
    }
    HInvoice entity = mapper.toEntity(toCrupdate, true);
    if (!entity.getProducts().isEmpty()) {
      productJpaRepository.deleteAll(entity.getProducts());
    }
    entity.setProducts(getProductEntities(toCrupdate, entity));
    //TODO: put this in the appropriate event service when async is set
    processPdfGeneration(mapper.toDomain(entity), entity);
    HInvoice savedEntity = jpaRepository.save(entity);
    return mapper.toDomain(savedEntity);
  }

  private void processPdfGeneration(Invoice domain, HInvoice entity) {
    String fileId = entity.getFileId() == null ? String.valueOf(randomUUID())
        : entity.getFileId();
    String accountId = domain.getAccount().getId();
    AccountHolder accountHolder =
        holderService.getAccountHolderByAccountId(accountId);
    byte[] logoAsBytes = fileService.downloadOptionalFile(LOGO, accountId, userLogoFileId());
    byte[] fileAsBytes =
        domain.getStatus().equals(CONFIRMED) || domain.getStatus().equals(PAID)
            ? pdfUtils.generatePdf(domain, accountHolder, logoAsBytes, INVOICE_TEMPLATE)
            : pdfUtils.generatePdf(domain, accountHolder, logoAsBytes, DRAFT_TEMPLATE);
    FileInfo fileInfo = fileService.upload(fileId, INVOICE, accountId, fileAsBytes, null);
    entity.setFileId(fileInfo.getId());
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
  public Optional<Invoice> getOptionalById(String invoiceId) {
    Optional<HInvoice> optionalInvoice = jpaRepository.findById(invoiceId);
    if (optionalInvoice.isPresent()) {
      HInvoice invoice = optionalInvoice.get();
      return Optional.of(mapper.toDomain(invoice));
    }
    return Optional.empty();
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
}
