package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.event.model.CustomerCrupdated.Type.CREATE;
import static app.bpartners.api.endpoint.event.model.CustomerCrupdated.Type.UPDATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.io.ClassPathResource;

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

  @Test
  void uses_created_template_on_create_type() throws MessagingException, IOException {
    var customerCrupdated = customerCrupdatedMock(CREATE);

    subject.accept(customerCrupdated);

    assertEquals("customer_created_template_mail", capturedTemplateName());
  }

  @Test
  void uses_updated_template_on_update_type() throws MessagingException, IOException {
    var customerCrupdated = customerCrupdatedMock(UPDATE);

    subject.accept(customerCrupdated);

    assertEquals("customer_updated_mail", capturedTemplateName());
  }

  @Test
  void resolved_templates_exist_in_classpath() {
    assertTrue(new ClassPathResource("templates/customer_created_template_mail.html").exists());
    assertTrue(new ClassPathResource("templates/customer_updated_mail.html").exists());
  }

  private String capturedTemplateName() {
    var templateCaptor = ArgumentCaptor.forClass(String.class);
    verify(templateResolverEngineMock).parseTemplateResolver(templateCaptor.capture(), any());
    return templateCaptor.getValue();
  }

  private CustomerCrupdated customerCrupdatedMock(CustomerCrupdated.Type type)
      throws MessagingException, IOException {
    var customerCrupdated = mock(CustomerCrupdated.class);
    when(customerCrupdated.getSubject()).thenReturn("subject");
    when(customerCrupdated.getRecipientEmail()).thenReturn("recipient@email.com");
    when(customerCrupdated.getUser()).thenReturn(User.builder().build());
    when(customerCrupdated.getCustomer())
        .thenReturn(
            Customer.builder()
                .address("address")
                .zipCode(123)
                .city("city")
                .country("country")
                .location(Location.builder().build())
                .latestFullAddress("latest address")
                .build());
    when(customerCrupdated.getType()).thenReturn(type);
    when(banApiMock.fSearch(anyString())).thenReturn(null);
    when(templateResolverEngineMock.parseTemplateResolver(anyString(), any()))
        .thenReturn("template");
    doNothing().when(sesServiceMock).sendEmail(anyString(), any(), any(), any(), anyList());
    return customerCrupdated;
  }
}
