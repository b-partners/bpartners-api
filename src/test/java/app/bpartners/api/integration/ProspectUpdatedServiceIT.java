package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.ContactNature.PROSPECT;
import static app.bpartners.api.endpoint.rest.model.CustomerStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.CustomerType.INDIVIDUAL;
import static app.bpartners.api.endpoint.rest.model.CustomerType.PROFESSIONAL;
import static app.bpartners.api.endpoint.rest.model.ProspectStatus.CONVERTED;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_HOLDER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.service.ProspectService.defaultStatusHistory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import app.bpartners.api.endpoint.event.SesConf;
import app.bpartners.api.endpoint.event.gen.ProspectUpdated;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Location;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.model.prospect.ProspectStatusHistory;
import app.bpartners.api.service.CustomerService;
import app.bpartners.api.service.ProspectService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.ProspectUpdatedService;
import app.bpartners.api.service.utils.GeoUtils;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import javax.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ProspectUpdatedServiceIT extends MockedThirdParties {
  private static final String PROSPECT_8_ID = "prospect8_id";
  private static final String PROSPECT_9_ID = "prospect9_id";
  private static final String PROSPECT_10_ID = "prospect10_id";
  private static final String PROSPECT_8_CUSTOMER_1_ID = "prospect_8_customer_1_id";
  public static final Instant DEFAULT_INSTANT = Instant.parse("2023-02-01T00:00:00.00Z");
  @Autowired ProspectUpdatedService subject;

  @MockBean SesConf sesConf;
  @MockBean SesService sesService;
  @Autowired ProspectService prospectService;
  @Autowired CustomerService customerService;

  static Prospect prospect8() {
    return Prospect.builder()
        .idHolderOwner(JOE_DOE_ACCOUNT_HOLDER_ID)
        .id(PROSPECT_8_ID)
        .name("Johnny	Paul")
        .email("johnny@gmail.com")
        .phone("+261340465345")
        .address("30 Rue de la Montagne Sainte-Genevieve")
        .statusHistories(defaultStatusHistory())
        .townCode(null)
        .location(new Geojson().type(null).longitude(1.0).latitude(1.0))
        .rating(Prospect.ProspectRating.builder().value(-1.0D).build())
        .contactNature(PROSPECT)
        .build();
  }

  static Prospect setConverted(Prospect prospect) {
    ArrayList<ProspectStatusHistory> statusHistories =
        new ArrayList<>(prospect.getStatusHistories());
    statusHistories.add(
        ProspectStatusHistory.builder()
            .updatedAt(DEFAULT_INSTANT.plus(Duration.ofSeconds(1)))
            .status(CONVERTED)
            .build());
    prospect.setStatusHistories(statusHistories);
    return prospect;
  }

  static Prospect prospect9() {
    return Prospect.builder()
        .idHolderOwner(JOE_DOE_ACCOUNT_HOLDER_ID)
        .id(PROSPECT_9_ID)
        .name("Johnny	Pauline")
        .email("johnnyp@gmail.com")
        .phone("+261340465346")
        .address("30 Rue de la Montagne Sainte-Genevieve")
        .statusHistories(defaultStatusHistory())
        .townCode(null)
        .location(new Geojson().type(null).longitude(1.0).latitude(1.0))
        .rating(Prospect.ProspectRating.builder().value(-1.0D).build())
        .contactNature(PROSPECT)
        .build();
  }

  static Prospect prospect10() {
    return Prospect.builder()
        .idHolderOwner(JOE_DOE_ACCOUNT_HOLDER_ID)
        .id(PROSPECT_10_ID)
        .name("Johnny	Paulinette")
        .email("johnnypette@gmail.com")
        .phone("+261340465347")
        .address("30 Rue de la Montagne Sainte-Genevieve")
        .statusHistories(defaultStatusHistory())
        .townCode(null)
        .location(null)
        .rating(Prospect.ProspectRating.builder().value(-1.0D).build())
        .contactNature(PROSPECT)
        .build();
  }

  static Customer prospect8Customer() {
    return Customer.builder()
        .id(PROSPECT_8_CUSTOMER_1_ID)
        .firstName("Johnny Paul")
        .lastName("")
        .idUser("joe_doe_id")
        .email("johnny@gmail.com")
        .phone("+261340465345")
        .website("https://johnny.website.com")
        .address("30 Rue de la Montagne Sainte-Genevieve")
        .zipCode(95160)
        .city("Metz")
        .country(null)
        .comment("Rencontre avec Johnny")
        .location(
            Location.builder()
                .address("30 Rue de la Montagne Sainte-Genevieve")
                .longitude(0d)
                .latitude(0d)
                .coordinate(GeoUtils.Coordinate.builder().longitude(0d).latitude(0d).build())
                .build())
        .status(ENABLED)
        .customerType(INDIVIDUAL)
        .build();
  }

  @Test
  void create_customer_ok() throws MessagingException, IOException {
    Prospect actualProspect9 = prospectService.getById(PROSPECT_9_ID);
    Optional<Customer> optionalLinkedCustomer = customerService.getByProspectId(PROSPECT_9_ID);
    String prospectUpdatedHtmlSubject =
        "Le prospect intitulé Johnny\tPauline appartenant à l'artisan NUMER est passé en statut À"
            + " CONTACTER le 01/02/2023 01:00";
    ProspectUpdated payload = new ProspectUpdated(prospect9(), DEFAULT_INSTANT);

    subject.accept(payload);

    assertTrue(optionalLinkedCustomer.isEmpty());
    assertEquals(ignoreHistoryUpdatedOf(prospect9()), ignoreHistoryUpdatedOf(actualProspect9));
    verify(sesService, times(1))
        .sendEmail(eq(null), eq(null), eq(prospectUpdatedHtmlSubject), any(), anyList());
    Optional<Customer> optionalLinkedCustomerAfterInsert =
        customerService.getByProspectId(PROSPECT_9_ID);
    assertTrue(optionalLinkedCustomerAfterInsert.isPresent());
    assertEquals(
        createCustomerFrom(prospect9()),
        ignoreIdOf(Objects.requireNonNull(ignoreDatesOf(optionalLinkedCustomerAfterInsert.get()))));
  }

  @Test
  void create_customer_from_locationless_prospect_ok() throws MessagingException, IOException {
    Prospect actualProspect10 = prospectService.getById(PROSPECT_10_ID);
    Optional<Customer> optionalLinkedCustomer = customerService.getByProspectId(PROSPECT_10_ID);
    String prospectUpdatedHtmlSubject =
        "Le prospect intitulé Johnny\tPaulinette appartenant à l'artisan NUMER est passé en statut"
            + " À CONTACTER le 01/02/2023 01:00";
    Prospect payloadProspect = prospect10();
    ProspectUpdated payload = new ProspectUpdated(payloadProspect, DEFAULT_INSTANT);

    subject.accept(payload);

    assertTrue(optionalLinkedCustomer.isEmpty());
    assertEquals(ignoreHistoryUpdatedOf(payloadProspect), ignoreHistoryUpdatedOf(actualProspect10));
    verify(sesService, times(1))
        .sendEmail(eq(null), eq(null), eq(prospectUpdatedHtmlSubject), any(), anyList());
    Optional<Customer> optionalLinkedCustomerAfterInsert =
        customerService.getByProspectId(PROSPECT_10_ID);
    assertTrue(optionalLinkedCustomerAfterInsert.isPresent());
    assertEquals(
        createCustomerFrom(payloadProspect),
        ignoreIdOf(Objects.requireNonNull(ignoreDatesOf(optionalLinkedCustomerAfterInsert.get()))));
  }

  @Test
  void update_customer_ok() throws MessagingException, IOException {
    Prospect actualProspect8 = prospectService.getById(PROSPECT_8_ID);
    Customer actualProspect8Customer = customerService.getCustomerById(PROSPECT_8_CUSTOMER_1_ID);
    String prospectUpdatedHtmlSubject =
        "Le prospect intitulé Johnny\tPaul appartenant à l'artisan NUMER est passé en statut À"
            + " CONTACTER le 01/02/2023 01:00";
    Prospect updatedProspect8 = setConverted(prospect8());
    ProspectUpdated payload = new ProspectUpdated(updatedProspect8, DEFAULT_INSTANT);

    subject.accept(payload);

    assertEquals(ignoreHistoryUpdatedOf(prospect8()), ignoreHistoryUpdatedOf(actualProspect8));
    assertEquals(ignoreDatesOf(prospect8Customer()), ignoreDatesOf(actualProspect8Customer));
    verify(sesService, times(1))
        .sendEmail(eq(null), eq(null), eq(prospectUpdatedHtmlSubject), any(), anyList());
    Optional<Customer> optionalLinkedCustomerAfterUpdate =
        customerService.getByProspectId(PROSPECT_8_ID);
    assertTrue(optionalLinkedCustomerAfterUpdate.isPresent());
    assertEquals(
        updateCustomerFrom(actualProspect8Customer, updatedProspect8),
        Objects.requireNonNull(ignoreDatesOf(optionalLinkedCustomerAfterUpdate.get())));
  }

  private static Prospect ignoreHistoryUpdatedOf(Prospect prospect) {
    prospect.getStatusHistories().forEach(history -> history.setUpdatedAt(null));
    return prospect;
  }

  private static Customer ignoreIdOf(Customer customer) {
    customer.setId(null);
    return customer;
  }

  private static Customer ignoreDatesOf(Customer customer) {
    customer.setUpdatedAt(null);
    customer.setCreatedAt(null);
    return customer;
  }

  private static Customer createCustomerFrom(Prospect prospect) {
    var prospectLocation = prospect.getLocation();
    Double longitude = prospectLocation == null ? null : prospectLocation.getLongitude();
    Double latitude = prospectLocation == null ? null : prospectLocation.getLatitude();
    Location location =
        Location.builder()
            .address(prospect.getAddress())
            .longitude(longitude)
            .latitude(latitude)
            .coordinate(
                GeoUtils.Coordinate.builder().longitude(longitude).latitude(latitude).build())
            .build();
    return Customer.builder()
        .id(null)
        .email(prospect.getEmail())
        .name(prospect.getName())
        .address(prospect.getAddress())
        .phone(prospect.getPhone())
        .comment(prospect.getComment())
        .isConverted(prospect.getActualStatus().equals(CONVERTED))
        .location(location)
        .idUser(JOE_DOE_ID)
        .status(ENABLED)
        // TODO: this defaults to PROFESSIONAL for now
        .customerType(PROFESSIONAL)
        .build();
  }

  private static Customer updateCustomerFrom(Customer original, Prospect prospect) {
    original.setConverted(CONVERTED.equals(prospect.getActualStatus()));
    original.setLatestFullAddress(original.getFullAddress());
    return original;
  }
}
