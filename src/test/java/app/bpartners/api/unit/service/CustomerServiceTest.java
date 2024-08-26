package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CustomerServiceTest {

  @Mock private CustomerRepository repository;

  @Mock private BanApi banApi;

  @InjectMocks private CustomerService customerService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testUpdateCustomersLocationAddressTooShort() {
    Customer customer = Customer.builder().id("1").address("12").build();
    List<Customer> customers = Collections.singletonList(customer);

    when(repository.findWhereLatitudeOrLongitudeIsNull()).thenReturn(customers);

    customerService.updateCustomersLocation();
    verify(repository, never()).save(any(Customer.class));
  }

  @Test
  void testUpdateCustomersLocationAddressTooLong() {
    Customer customer = Customer.builder().id("1").address("A".repeat(201)).build();
    List<Customer> customers = Collections.singletonList(customer);

    when(repository.findWhereLatitudeOrLongitudeIsNull()).thenReturn(customers);

    customerService.updateCustomersLocation();
    verify(repository, never()).save(any(Customer.class));
  }

  @Test
  void testUpdateCustomersLocationAddressNotFound() {
    Customer customer = Customer.builder().id("1").address("123 Street").build();
    List<Customer> customers = Collections.singletonList(customer);

    when(repository.findWhereLatitudeOrLongitudeIsNull()).thenReturn(customers);
    when(banApi.search("123 Street")).thenReturn(null);

    customerService.updateCustomersLocation();
    verify(repository, never()).save(any(Customer.class));
  }

  @Test
  void testUpdateCustomersLocationExceptionHandling() {
    Customer customer = Customer.builder().id("1").address("123 Street").build();
    List<Customer> customers = Collections.singletonList(customer);

    when(repository.findWhereLatitudeOrLongitudeIsNull()).thenReturn(customers);
    when(banApi.search("123 Street")).thenThrow(new BadRequestException("Bad Request"));

    customerService.updateCustomersLocation();
    verify(repository, never()).save(any(Customer.class));
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
    when(banApi.search("123 Main St, Springfield")).thenReturn(geoPosition);
    when(repository.findWhereLatitudeOrLongitudeIsNull()).thenReturn(List.of(customer));

    customerService.updateCustomersLocation();
    verify(customer, times(1)).setLocation(any(Location.class));
    verify(repository, times(1)).save(customer);
  }

  @Test
  void testUpdateCustomersLocationInvalidAddress() {
    Customer customer = mock(Customer.class);

    when(customer.getId()).thenReturn("1");
    when(customer.getFullAddress()).thenReturn("12");
    when(customer.getAddress()).thenReturn("12");
    when(repository.findWhereLatitudeOrLongitudeIsNull()).thenReturn(List.of(customer));

    customerService.updateCustomersLocation();
    verify(banApi, never()).search(anyString());
    verify(customer, never()).setLocation(any(Location.class));
    verify(repository, never()).save(customer);
  }
}
