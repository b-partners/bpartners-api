package app.bpartners.api.unit.service;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static org.junit.jupiter.api.Assertions.assertTrue;
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

import nl.altindag.log.LogCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CustomerServiceTest {
  CustomerRepository repositoryMock;
  BanApi banApiMock;
  CustomerRestMapper restMapperMock;
  EventProducer eventProducerMock;
  EventConf eventConfMock;
  SesConf sesConfMock;
  CustomerService subject;
  LogCaptor logCaptor;

  @BeforeEach
  void setUp() {
    logCaptor = LogCaptor.forClass(CustomerService.class);
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

  Customer.CustomerBuilder customerBuilder() {
    return Customer.builder().id(JOE_DOE_ID);
  }

  @Test
  void test_update_customers_location_address_to_long() {
    var customer = customerBuilder().address("12".repeat(200)).build();
    when(repositoryMock.findWhereLatitudeOrLongitudeIsNull()).thenReturn(List.of(customer));

    subject.updateCustomersLocation();
    var actual = logCaptor.getWarnLogs();

    var expected = String.format(
            "Unable to update Customer(id=%s,name=%s) position because address was %s",
            customer.getId(), customer.getFullName(), customer.getFullAddress());
    assertTrue(actual.contains(expected));
  }

  @Test
  void test_update_customers_location_address_not_found() {
    var customer = customerBuilder().address("123 Street").build();
    when(repositoryMock.findWhereLatitudeOrLongitudeIsNull()).thenReturn(List.of(customer));
    when(banApiMock.search("123 Street")).thenReturn(null);

    subject.updateCustomersLocation();
    var actual = logCaptor.getWarnLogs();

    var expected = String.format("Customer(id=%s) location was not updated because address %s was not found",
            customer.getId(), customer.getFullAddress());
    assertTrue(actual.contains(expected));
  }

  @Test
  void test_update_customers_location_bad_request() {
    var customer = customerBuilder().address("123 Street").build();
    when(repositoryMock.findWhereLatitudeOrLongitudeIsNull()).thenReturn(List.of(customer));
    when(banApiMock.search(any())).thenThrow(new BadRequestException("Bad Request"));

    subject.updateCustomersLocation();

    verify(repositoryMock, never()).save(any(Customer.class));
  }

  @Test
  void test_update_customers_location_successful_update() {
    var coordinates = GeoUtils.Coordinate.builder().latitude(40.7128).longitude(-74.0060).build();
    var geoPosition = GeoPosition.builder().coordinates(coordinates).build();
    var customer =
        Customer.builder()
            .address("123 Main St, Springfield")
            .zipCode(103)
            .city("")
            .country("")
            .build();
    when(banApiMock.search(any())).thenReturn(geoPosition);
    when(repositoryMock.findWhereLatitudeOrLongitudeIsNull()).thenReturn(List.of(customer));

    subject.updateCustomersLocation();

    verify(repositoryMock, times(1)).save(customer);
  }

  @Test
  void test_update_customers_location_invalid_address() {
    var customer = Customer.builder().address("").zipCode(10).city("").country("").build();
    when(repositoryMock.findWhereLatitudeOrLongitudeIsNull()).thenReturn(List.of(customer));

    subject.updateCustomersLocation();

    verify(repositoryMock, never()).save(customer);
  }
}
