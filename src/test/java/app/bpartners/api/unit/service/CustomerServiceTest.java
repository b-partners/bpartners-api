package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.*;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.service.CustomerService;
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
  void testUpdateCustomersLocation_AddressTooShort() {
    Customer customer = Customer.builder().id("1").address("12").build();
    List<Customer> customers = Collections.singletonList(customer);

    when(repository.findWhereLatitudeOrLongitudeIsNull()).thenReturn(customers);

    customerService.updateCustomersLocation();
    verify(repository, never()).save(any(Customer.class));
  }

  @Test
  void testUpdateCustomersLocation_AddressTooLong() {
    Customer customer = Customer.builder().id("1").address("A".repeat(201)).build();
    List<Customer> customers = Collections.singletonList(customer);

    when(repository.findWhereLatitudeOrLongitudeIsNull()).thenReturn(customers);

    customerService.updateCustomersLocation();
    verify(repository, never()).save(any(Customer.class));
  }

  @Test
  void testUpdateCustomersLocation_AddressNotFound() {
    Customer customer = Customer.builder().id("1").address("123 Street").build();
    List<Customer> customers = Collections.singletonList(customer);

    when(repository.findWhereLatitudeOrLongitudeIsNull()).thenReturn(customers);
    when(banApi.search("123 Street")).thenReturn(null);

    customerService.updateCustomersLocation();
    verify(repository, never()).save(any(Customer.class));
  }

  @Test
  void testUpdateCustomersLocation_ExceptionHandling() {
    Customer customer = Customer.builder().id("1").address("123 Street").build();
    List<Customer> customers = Collections.singletonList(customer);

    when(repository.findWhereLatitudeOrLongitudeIsNull()).thenReturn(customers);
    when(banApi.search("123 Street")).thenThrow(new BadRequestException("Bad Request"));

    customerService.updateCustomersLocation();
    verify(repository, never()).save(any(Customer.class));
  }
}
