package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.model.Customer;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;

class CustomerCreatedTemplateMailTest {
  private static final String TEMPLATE = "customer_created_template_mail";
  TemplateResolverEngine subject = new TemplateResolverEngine();

  @Test
  void renders_customer_information() {
    var actual = subject.parseTemplateResolver(TEMPLATE, contextOf(customer()));

    assertTrue(actual.contains("Dupont"));
    assertTrue(actual.contains("Boulangerie Dupont"));
    assertTrue(actual.contains("contact@dupont.fr"));
    assertTrue(actual.contains("+33600000000"));
    assertTrue(actual.contains("14 rue Soleillet"));
    assertTrue(actual.contains("Paris"));
    assertTrue(actual.contains("75020"));
    assertTrue(actual.contains("France"));
    assertTrue(actual.contains("https://dupont.fr"));
  }

  @Test
  void renders_full_address_and_booking_link() {
    var actual = subject.parseTemplateResolver(TEMPLATE, contextOf(customer()));

    assertTrue(actual.contains(customer().getFullAddress()));
    assertTrue(actual.contains("https://meet.brevo.com/birdia/reunion-de-15-minutes"));
  }

  @Test
  void renders_birdia_signature() {
    var actual = subject.parseTemplateResolver(TEMPLATE, contextOf(customer()));

    assertTrue(actual.contains("L'équipe BIRDIA"));
    assertTrue(actual.contains("06 68 62 48 36"));
    assertTrue(actual.contains("https://www.birdia.fr/"));
  }

  @Test
  void renders_without_throwing_when_customer_fields_are_null() {
    assertDoesNotThrow(
        () -> subject.parseTemplateResolver(TEMPLATE, contextOf(Customer.builder().build())));
  }

  private Context contextOf(Customer customer) {
    Context context = new Context();
    context.setVariable("customer", customer);
    return context;
  }

  private Customer customer() {
    return Customer.builder()
        .name("Boulangerie Dupont")
        .lastName("Dupont")
        .email("contact@dupont.fr")
        .phone("+33600000000")
        .address("14 rue Soleillet")
        .zipCode(75020)
        .city("Paris")
        .country("France")
        .website("https://dupont.fr")
        .build();
  }
}
