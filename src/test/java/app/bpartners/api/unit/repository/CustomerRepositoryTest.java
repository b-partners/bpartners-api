package app.bpartners.api.unit.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.model.Customer;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.implementation.CustomerRepositoryImpl;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class CustomerRepositoryTest {

  @Mock private CustomerRepository customerRepository;

  @InjectMocks private CustomerRepositoryImpl customerRepositoryImpl;

  private Customer customer;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    customer =
        Customer.builder()
            .id("1")
            .idAccount("account1")
            .idUser("user1")
            .name("John Doe")
            .email("john.doe@example.com")
            .phone("123456789")
            .address("123 Main St")
            .zipCode(12345)
            .city("Anytown")
            .country("Countryland")
            .status(CustomerStatus.ENABLED)
            .build();
  }

  @Test
  void findAllByIdUserOrderByLastNameAsc_returnsCustomerList() {
    when(customerRepository.findAllByIdUserOrderByLastNameAsc("user1"))
        .thenReturn(List.of(customer));

    List<Customer> customers = customerRepository.findAllByIdUserOrderByLastNameAsc("user1");
    assertNotNull(customers);
    assertEquals(1, customers.size());
    assertEquals(customer, customers.get(0));
  }

  @Test
  void findByIdAccountHolder_returnsCustomerList() {
    when(customerRepository.findByIdAccountHolder("account1")).thenReturn(List.of(customer));

    List<Customer> customers = customerRepository.findByIdAccountHolder("account1");
    assertNotNull(customers);
    assertEquals(1, customers.size());
    assertEquals(customer, customers.get(0));
  }

  @Test
  void saveAll_returnsSavedCustomers() {
    when(customerRepository.saveAll(List.of(customer))).thenReturn(List.of(customer));

    List<Customer> savedCustomers = customerRepository.saveAll(List.of(customer));
    assertNotNull(savedCustomers);
    assertEquals(1, savedCustomers.size());
    assertEquals(customer, savedCustomers.get(0));
  }

  @Test
  void save_returnsSavedCustomer() {
    when(customerRepository.save(customer)).thenReturn(customer);

    Customer savedCustomer = customerRepository.save(customer);
    assertNotNull(savedCustomer);
    assertEquals(customer, savedCustomer);
  }

  @Test
  void findById_returnsCustomer() {
    when(customerRepository.findById("1")).thenReturn(customer);

    Customer foundCustomer = customerRepository.findById("1");
    assertNotNull(foundCustomer);
    assertEquals(customer, foundCustomer);
  }

  @Test
  void findOptionalByProspectId_returnsOptionalCustomer() {
    when(customerRepository.findOptionalByProspectId("prospect1"))
        .thenReturn(Optional.of(customer));

    Optional<Customer> optionalCustomer = customerRepository.findOptionalByProspectId("prospect1");
    assertTrue(optionalCustomer.isPresent());
    assertEquals(customer, optionalCustomer.get());
  }

  @Test
  void findOptionalById_returnsOptionalCustomer() {
    when(customerRepository.findOptionalById("1")).thenReturn(Optional.of(customer));

    Optional<Customer> optionalCustomer = customerRepository.findOptionalById("1");
    assertTrue(optionalCustomer.isPresent());
    assertEquals(customer, optionalCustomer.get());
  }

  @Test
  void findByIdUserAndCriteria_returnsFilteredCustomerList() {
    when(customerRepository.findByIdUserAndCriteria(
            "user1",
            "John",
            "Doe",
            "john.doe@example.com",
            "123456789",
            "Anytown",
            "Countryland",
            List.of(),
            "prospect1",
            CustomerStatus.ENABLED,
            0,
            10))
        .thenReturn(List.of(customer));

    List<Customer> customers =
        customerRepository.findByIdUserAndCriteria(
            "user1",
            "John",
            "Doe",
            "john.doe@example.com",
            "123456789",
            "Anytown",
            "Countryland",
            List.of(),
            "prospect1",
            CustomerStatus.ENABLED,
            0,
            10);
    assertNotNull(customers);
    assertEquals(1, customers.size());
    assertEquals(customer, customers.get(0));
  }

  @Test
  void updateCustomersStatuses_returnsUpdatedCustomers() {
    when(customerRepository.updateCustomersStatuses(List.of(new UpdateCustomerStatus())))
        .thenReturn(List.of(customer.toBuilder().status(CustomerStatus.DISABLED).build()));

    List<Customer> updatedCustomers =
        customerRepository.updateCustomersStatuses(List.of(new UpdateCustomerStatus()));
    assertNotNull(updatedCustomers);
    assertEquals(1, updatedCustomers.size());
    assertEquals(CustomerStatus.DISABLED, updatedCustomers.get(0).getStatus());
  }

  @Test
  void findWhereLatitudeOrLongitudeIsNull_returnsCustomers() {
    when(customerRepository.findWhereLatitudeOrLongitudeIsNull()).thenReturn(List.of(customer));

    List<Customer> customers = customerRepository.findWhereLatitudeOrLongitudeIsNull();
    assertNotNull(customers);
    assertEquals(1, customers.size());
    assertEquals(customer, customers.get(0));
  }
}
