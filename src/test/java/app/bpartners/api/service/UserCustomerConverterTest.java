package app.bpartners.api.service;

import static app.bpartners.api.endpoint.rest.model.CustomerStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.CustomerType.PROFESSIONAL;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.User;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.service.customer.UserCustomerConverter;
import org.junit.jupiter.api.Test;

class UserCustomerConverterTest {
  UserSubscriptionConf userSubscriptionConfMock = mock();
  CustomerRepository customerRepositoryMock = mock();
  UserCustomerConverter subject =
      new UserCustomerConverter(userSubscriptionConfMock, customerRepositoryMock);

  @Test
  void convert_user_to_customer() {
    var userOwnderIdentifier = randomUUID().toString();
    var accountHolderName = "accountHolderName";
    var accountHolderPhone = "0223456789";
    var userEmail = "user@example.com";
    var website = "https://example.com";
    var lineAddress = "123 Main St";
    var zipCode = "12345";
    var city = "New York";
    var country = "US";
    var userFirstName = "John";
    var userLastName = "Doe";
    var userMock = mock(User.class);
    when(userMock.getEmail()).thenReturn(userEmail);
    when(userMock.getFirstName()).thenReturn(userFirstName);
    when(userMock.getLastName()).thenReturn(userLastName);
    var accountHolderMock = mock(AccountHolder.class);
    when(accountHolderMock.getMobilePhoneNumber()).thenReturn(accountHolderPhone);
    when(accountHolderMock.getWebsite()).thenReturn(website);
    when(accountHolderMock.getAddress()).thenReturn(lineAddress);
    when(accountHolderMock.getPostalCode()).thenReturn(zipCode);
    when(accountHolderMock.getCity()).thenReturn(city);
    when(accountHolderMock.getCountry()).thenReturn(country);
    when(accountHolderMock.getName()).thenReturn(accountHolderName);
    when(userMock.getDefaultHolder()).thenReturn(accountHolderMock);
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(userOwnderIdentifier);
    when(customerRepositoryMock.save(any())).thenAnswer(i -> i.getArgument(0));

    var actual = subject.apply(userMock);

    assertEquals(
        Customer.builder()
            .id(actual.getId())
            .idUser(userOwnderIdentifier)
            .name(accountHolderName)
            .firstName(userFirstName)
            .lastName(userLastName)
            .email(userEmail)
            .phone(accountHolderPhone)
            .website(website)
            .address(lineAddress)
            .zipCode(Integer.valueOf(zipCode))
            .city(city)
            .country(country)
            .comment(null)
            .location(null)
            .status(ENABLED)
            .customerType(PROFESSIONAL)
            .recentlyAdded(false)
            .updatedAt(actual.getUpdatedAt())
            .createdAt(actual.getCreatedAt())
            .build(),
        actual);
  }
}
