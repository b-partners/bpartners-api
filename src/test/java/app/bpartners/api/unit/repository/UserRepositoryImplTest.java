package app.bpartners.api.unit.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.bridge.repository.BridgeUserRepository;
import app.bpartners.api.repository.implementation.UserRepositoryImpl;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HUser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserRepositoryImplTest {
  EntityManager entityManagerMock = mock();
  BankRepository bankRepositoryMock = mock();
  AccountJpaRepository accountJpaRepositoryMock = mock();
  AccountHolderJpaRepository holderJpaRepositoryMock = mock();
  BridgeUserRepository bridgeUserRepositoryMock = mock();
  CognitoComponent cognitoComponentMock = mock();
  UserMapper userMapperMock = mock();
  UserJpaRepository userJpaRepositoryMock = mock();
  UserRepositoryImpl subject =
      new UserRepositoryImpl(
          userJpaRepositoryMock,
          userMapperMock,
          cognitoComponentMock,
          bridgeUserRepositoryMock,
          holderJpaRepositoryMock,
          accountJpaRepositoryMock,
          bankRepositoryMock,
          entityManagerMock);

  @Test
  void find_all_by_criteria_with_default_page_and_size_values() {
    var criteriaBuilderMock = mock(CriteriaBuilder.class);
    var queryMock = mock(CriteriaQuery.class);
    var rootMock = mock(Root.class);
    var predicateMock = mock(Predicate.class);
    var typedQueryMock = mock(TypedQuery.class);
    var userEntityMock = mock(HUser.class);
    var userMock = mock(User.class);
    var expectedUsers = List.of(userMock);
    var expectedPage = 1;
    int expectedPageSize = 500;
    var criteria = new HashMap<String, Object>();
    criteria.put("status", EnableStatus.ENABLED);

    when(userMapperMock.toDomain(any())).thenReturn(userMock);
    when(queryMock.from(HUser.class)).thenReturn(rootMock);
    when(criteriaBuilderMock.createQuery(HUser.class)).thenReturn(queryMock);
    when(criteriaBuilderMock.equal(any(Expression.class), any())).thenReturn(predicateMock);
    when(queryMock.where(predicateMock)).thenReturn(queryMock);
    when(entityManagerMock.getCriteriaBuilder()).thenReturn(criteriaBuilderMock);
    when(typedQueryMock.setFirstResult(eq(expectedPage - 1))).thenReturn(typedQueryMock);
    when(typedQueryMock.setMaxResults(eq(expectedPageSize))).thenReturn(typedQueryMock);
    when(typedQueryMock.getResultList()).thenReturn(List.of(userEntityMock));
    when(entityManagerMock.createQuery(queryMock)).thenReturn(typedQueryMock);

    var actual = subject.findAllByCriteria(criteria);

    var integerCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(typedQueryMock).setFirstResult(integerCaptor.capture());
    verify(typedQueryMock).setMaxResults(integerCaptor.capture());
    var capturedPageValue = integerCaptor.getAllValues().getFirst();
    var capturedPageSizeValue = integerCaptor.getAllValues().getLast();

    assertEquals(expectedUsers, actual);
    assertEquals(0, capturedPageValue);
    assertEquals(expectedPageSize, capturedPageSizeValue);
  }

  @Test
  void find_all_by_criteria_with_provided_page_and_size_values() {
    var criteriaBuilderMock = mock(CriteriaBuilder.class);
    var queryMock = mock(CriteriaQuery.class);
    var rootMock = mock(Root.class);
    var predicateMock = mock(Predicate.class);
    var typedQueryMock = mock(TypedQuery.class);
    var userEntityMock = mock(HUser.class);
    var userMock = mock(User.class);
    var expectedUsers = List.of(userMock);
    var expectedPage = 2L;
    var expectedPageSize = 99L;
    var criteria = new HashMap<String, Object>();
    criteria.put("status", EnableStatus.ENABLED);
    criteria.put("page", expectedPage);
    criteria.put("pageSize", expectedPageSize);

    when(userMapperMock.toDomain(any())).thenReturn(userMock);
    when(queryMock.from(HUser.class)).thenReturn(rootMock);
    when(criteriaBuilderMock.createQuery(HUser.class)).thenReturn(queryMock);
    when(criteriaBuilderMock.equal(any(Expression.class), any())).thenReturn(predicateMock);
    when(queryMock.where(predicateMock)).thenReturn(queryMock);
    when(entityManagerMock.getCriteriaBuilder()).thenReturn(criteriaBuilderMock);
    when(typedQueryMock.setFirstResult(eq((1)))).thenReturn(typedQueryMock);
    when(typedQueryMock.setMaxResults(eq(99))).thenReturn(typedQueryMock);
    when(typedQueryMock.getResultList()).thenReturn(List.of(userEntityMock));
    when(entityManagerMock.createQuery(queryMock)).thenReturn(typedQueryMock);

    var actual = subject.findAllByCriteria(criteria);

    var integerCaptor = ArgumentCaptor.forClass(Integer.class);
    verify(typedQueryMock).setFirstResult(integerCaptor.capture());
    verify(typedQueryMock).setMaxResults(integerCaptor.capture());
    var capturedPageValue = integerCaptor.getAllValues().getFirst();
    var capturedPageSizeValue = integerCaptor.getAllValues().getLast();

    assertEquals(expectedUsers, actual);
    assertEquals(1, capturedPageValue);
    assertEquals(expectedPageSize, capturedPageSizeValue.intValue());
  }

  @Test
  void find_all_by_criteria_with_not_supported_criteria() {
    var criteriaBuilderMock = mock(CriteriaBuilder.class);
    var queryMock = mock(CriteriaQuery.class);
    var rootMock = mock(Root.class);
    var typedQueryMock = mock(TypedQuery.class);
    var criteriaKey = "name";
    var expectedMessage =
        "Only email, status, page and pageSize criteria filter are handled for now but criteria was"
            + " "
            + criteriaKey;
    var criteria = new HashMap<String, Object>();
    criteria.put(criteriaKey, "dummy");

    when(queryMock.from(HUser.class)).thenReturn(rootMock);
    when(criteriaBuilderMock.createQuery(HUser.class)).thenReturn(queryMock);
    when(entityManagerMock.getCriteriaBuilder()).thenReturn(criteriaBuilderMock);
    when(entityManagerMock.createQuery(queryMock)).thenReturn(typedQueryMock);

    var actual =
        assertThrows(NotImplementedException.class, () -> subject.findAllByCriteria(criteria));

    assertEquals(expectedMessage, actual.getMessage());
  }
}
