package app.bpartners.api.repository.implementation;

import static org.springframework.data.domain.Sort.Direction.DESC;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.ArchiveInvoice;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.InvoiceProductMapper;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
  private final InvoiceMapper mapper;
  private final InvoiceProductMapper productMapper;
  private final EntityManager entityManager;
  private final UserRepository userRepository;
  private final PaymentRequestMapper paymentRequestMapper;

  @Override
  public List<Invoice> findAllEnabledByIdUser(String idUser) {
    return jpaRepository.findAllByIdUserAndArchiveStatus(idUser, ArchiveStatus.ENABLED).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public Invoice save(Invoice toSave) {
    var paymentRequests =
        new ArrayList<>(
            toSave.getPaymentRegulations().stream().map(paymentRequestMapper::toEntity).toList());
    var invoiceProducts = toSave.getProducts().stream().map(productMapper::toEntity).toList();
    var savedInvoice =
        jpaRepository.save(mapper.toEntity(toSave, paymentRequests, invoiceProducts));
    return mapper.toDomain(savedInvoice, userRepository.getById(savedInvoice.getIdUser()));
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
  public List<Invoice> archiveAll(List<ArchiveInvoice> archiveInvoices) {
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
    var invoice =
        jpaRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Invoice(id=" + id + ") not found"));
    var user = userRepository.getById(invoice.getIdUser());
    return mapper.toDomain(invoice, user);
  }
}
