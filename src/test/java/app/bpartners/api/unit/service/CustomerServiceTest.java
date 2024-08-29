package app.bpartners.api.unit.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventConf;
import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.rest.mapper.CustomerRestMapper;
import app.bpartners.api.model.*;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.service.CustomerService;
import app.bpartners.api.service.utils.GeoUtils;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomerServiceTest {
  CustomerRepository repositoryMock;
  BanApi banApiMock;
  CustomerRestMapper restMapperMock;
  EventProducer eventProducerMock;
  EventConf eventConfMock;
  SesConf sesConfMock;
  CustomerService subject;

  @BeforeEach
  void setUp() {
    repositoryMock = mock(CustomerRepository.class);
    banApiMock = mock(BanApi.class);
    subject =
        new CustomerService(
            repositoryMock,
            restMapperMock,
            eventProducerMock,
            eventConfMock,
            sesConfMock,
            banApiMock);
  }

  @Test
  void testUpdateCustomersLocationAddressTooShort() {
    Customer customer = Customer.builder().id("1").address("12").build();
    List<Customer> customers = Collections.singletonList(customer);

    when(repositoryMock.findWhereLatitudeOrLongitudeIsNull()).thenReturn(customers);

    subject.updateCustomersLocation();
    verify(repositoryMock, never()).save(any(Customer.class));
  }

  @Test
  void testUpdateCustomersLocationAddressTooLong() {
    Customer customer = Customer.builder().id("1").address("A".repeat(201)).build();
    List<Customer> customers = Collections.singletonList(customer);

    when(repositoryMock.findWhereLatitudeOrLongitudeIsNull()).thenReturn(customers);

    subject.updateCustomersLocation();
    verify(repositoryMock, never()).save(any(Customer.class));
  }

  @Test
  void testUpdateCustomersLocationAddressNotFound() {
    Customer customer = Customer.builder().id("1").address("123 Street").build();
    List<Customer> customers = Collections.singletonList(customer);

    when(repositoryMock.findWhereLatitudeOrLongitudeIsNull()).thenReturn(customers);
    when(banApiMock.search("123 Street")).thenReturn(null);

    subject.updateCustomersLocation();
    verify(repositoryMock, never()).save(any(Customer.class));
  }

  @Test
  void testUpdateCustomersLocationExceptionHandling() {
    Customer customer = Customer.builder().id("1").address("123 Street").build();
    List<Customer> customers = Collections.singletonList(customer);

    when(repositoryMock.findWhereLatitudeOrLongitudeIsNull()).thenReturn(customers);
    when(banApiMock.search("123 Street")).thenThrow(new BadRequestException("Bad Request"));

    subject.updateCustomersLocation();
    verify(repositoryMock, never()).save(any(Customer.class));
  }

  @Test
  void testUpdateCustomersLocationSuccessfulUpdate() {
    var geoPosition = mock(GeoPosition.class);
    GeoUtils.Coordinate coordinates = mock(GeoUtils.Coordinate.class);
    Customer customer = mock(Customer.class);

    when(customer.getId()).thenReturn("1");
    when(customer.getFullAddress()).thenReturn("123 Main St, Springfield");
    when(customer.getAddress()).thenReturn("123 Main St, Springfield");
    when(coordinates.getLatitude()).thenReturn(40.7128);
    when(coordinates.getLongitude()).thenReturn(-74.0060);
    when(geoPosition.getCoordinates()).thenReturn(coordinates);
    when(banApiMock.search("123 Main St, Springfield")).thenReturn(geoPosition);
    when(repositoryMock.findWhereLatitudeOrLongitudeIsNull()).thenReturn(List.of(customer));

    subject.updateCustomersLocation();
    verify(customer, times(1)).setLocation(any(Location.class));
    verify(repositoryMock, times(1)).save(customer);
  }

  @Test
  void testUpdateCustomersLocationInvalidAddress() {
    Customer customer = mock(Customer.class);

    when(customer.getId()).thenReturn("1");
    when(customer.getFullAddress()).thenReturn("12");
    when(customer.getAddress()).thenReturn("12");
    when(repositoryMock.findWhereLatitudeOrLongitudeIsNull()).thenReturn(List.of(customer));

    subject.updateCustomersLocation();
    verify(banApiMock, never()).search(anyString());
    verify(customer, never()).setLocation(any(Location.class));
    verify(repositoryMock, never()).save(customer);
  }
}
