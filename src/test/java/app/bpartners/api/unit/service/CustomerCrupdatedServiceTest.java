package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.event.model.CustomerCrupdated.Type.CREATE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.LogCaptor;
import app.bpartners.api.endpoint.event.model.CustomerCrupdated;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Location;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.CustomerCrupdatedService;
import app.bpartners.api.service.utils.GeoUtils;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import javax.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.OutputCaptureExtension;

@Slf4j
@ExtendWith(OutputCaptureExtension.class)
class CustomerCrupdatedServiceTest {
  SesService sesServiceMock = mock();
  CustomerRepository customerRepositoryMock = mock();
  BanApi banApiMock = mock();
  TemplateResolverEngine templateResolverEngineMock = mock();
  CustomerCrupdatedService subject =
      new CustomerCrupdatedService(
          sesServiceMock, customerRepositoryMock, banApiMock, templateResolverEngineMock);

  @Test
  void accept() throws MessagingException, IOException {
    var logCaptor = new LogCaptor();
    logCaptor.configure(CustomerCrupdatedService.class);
    var customerCrupdated = mock(CustomerCrupdated.class);
    when(customerCrupdated.getSubject()).thenReturn("subject");
    when(customerCrupdated.getRecipientEmail()).thenReturn("recipient@email.com");
    var user = User.builder().build();
    when(customerCrupdated.getUser()).thenReturn(user);
    var customer =
        Customer.builder()
            .address("address")
            .zipCode(123)
            .city("city")
            .country("country")
            .location(Location.builder().build())
            .latestFullAddress("latest address")
            .build();
    when(customerCrupdated.getCustomer()).thenReturn(customer);
    when(customerCrupdated.getType()).thenReturn(CREATE);
    var coordinate = GeoUtils.Coordinate.builder().latitude(1.1).longitude(20.2).build();
    var customerPosition = GeoPosition.builder().coordinates(coordinate).build();
    when(banApiMock.fSearch(anyString())).thenReturn(customerPosition);
    when(customerRepositoryMock.save(any())).thenReturn(customer);
    when(templateResolverEngineMock.parseTemplateResolver(anyString(), any()))
        .thenReturn("template");
    doNothing().when(sesServiceMock).sendEmail(anyString(), any(), any(), any(), anyList());

    subject.accept(customerCrupdated);

    verify(banApiMock, times(1)).fSearch(anyString());
    verify(customerRepositoryMock, times(1)).save(any());
    verify(templateResolverEngineMock, times(1)).parseTemplateResolver(anyString(), any());
    verify(sesServiceMock, times(1)).sendEmail(anyString(), any(), any(), any(), anyList());
  }
}
