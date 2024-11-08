package app.bpartners.api.unit.repository;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static java.time.LocalDate.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.InvoiceProductMapper;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.implementation.InvoiceRepositoryImpl;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;

class InvoiceRepositoryImplTest {
  InvoiceJpaRepository jpaRepositoryMock = mock(InvoiceJpaRepository.class);
  InvoiceMapper mapperMock = mock(InvoiceMapper.class);
  InvoiceProductMapper productMapperMock = mock(InvoiceProductMapper.class);
  EntityManager entityManagerMock = mock(EntityManager.class);
  UserRepository userRepositoryMock = mock(UserRepository.class);
  PaymentRequestMapper paymentRequestMapperMock = mock(PaymentRequestMapper.class);
  InvoiceRepositoryImpl subject =
      new InvoiceRepositoryImpl(
              jpaRepositoryMock,
              mapperMock,
              productMapperMock,
              entityManagerMock,
              userRepositoryMock,
              paymentRequestMapperMock);

  @Test
  void find_by_id() {
    var hInvoice = HInvoice.builder().idUser("").build();
    var user = new User();
    var invoice = Invoice.builder().build();
    when(jpaRepositoryMock.findById(any())).thenReturn(Optional.of(hInvoice));
    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(mapperMock.toDomain(any(), any())).thenReturn(invoice);

    var actual = subject.findById("");

    assertEquals(invoice, actual);
  }

    @Test
    void find_all_by_id_user_and_sending_date_between_and_paginate() {
      var from = now();
      var to = now();
      var page = 1;
      var builder = mock(CriteriaBuilder.class);
      when(entityManagerMock.getCriteriaBuilder()).thenReturn(builder);
      var query = mock(CriteriaQuery.class);
      when(builder.createQuery(any())).thenReturn(query);
      var root = mock(Root.class);
      var path = mock(Path.class);
      when(query.from(HInvoice.class)).thenReturn(root);
      when(root.get(anyString())).thenReturn(path);
      var predicates = mock(Predicate.class);
      when(builder.equal(any(), any())).thenReturn(predicates);
      when(builder.and(any())).thenReturn(predicates);
      var hInvoice = HInvoice.builder().build();
      var invoices = List.of(hInvoice);
      var queryEntityManager = mock(Query.class);
      when(entityManagerMock.createQuery(anyString())).thenReturn(queryEntityManager);
      when(queryEntityManager.setFirstResult(anyInt())).thenReturn(queryEntityManager);
      when(queryEntityManager.setMaxResults(anyInt())).thenReturn(queryEntityManager);
      when(entityManagerMock.createQuery(anyString()).setFirstResult(anyInt()).setMaxResults(anyInt()).getResultList()).thenReturn(invoices);

      var actual = subject.findAllByIdUserAndSendingDateBetweenAndPaginate(JOE_DOE_ID, from, to, page, MAX_SIZE);

      assertEquals(invoices, actual);
    }
}
