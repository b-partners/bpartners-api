package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.ContactNature.PROSPECT;
import static app.bpartners.api.endpoint.rest.model.CustomerStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.CustomerType.INDIVIDUAL;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_HOLDER_ID;
import static app.bpartners.api.service.ProspectService.defaultStatusHistory;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import app.bpartners.api.service.CustomerService;
import app.bpartners.api.service.ProspectService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.ProspectUpdatedService;
import app.bpartners.api.service.utils.GeoUtils;
import java.io.IOException;
import java.time.Instant;
import javax.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

public class ProspectUpdatedServiceIT extends MockedThirdParties {
  private static final String PROSPECT_8_ID = "prospect8_id";
  private static final String PROSPECT_8_CUSTOMER_1_ID = "prospect_8_customer_1_id";
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
  void update_customer_ok() throws MessagingException, IOException {
    Prospect actualProspect8 = prospectService.getById(PROSPECT_8_ID);
    Customer actualProspect8Customer = customerService.getCustomerById(PROSPECT_8_CUSTOMER_1_ID);
    assertEquals(ignoreHistoryUpdatedOf(prospect8()), ignoreHistoryUpdatedOf(actualProspect8));
    assertEquals(ignoreDatesOf(prospect8Customer()), ignoreDatesOf(actualProspect8Customer));

    ProspectUpdated payload =
        new ProspectUpdated(prospect8(), Instant.parse("2023-02-01T00:00:00.00Z"));

    subject.accept(payload);

    String prospectUpdatedHtmlSubject =
        "Le prospect intitulé Johnny\tPaul appartenant à l'artisan NUMER est passé en statut À"
            + " CONTACTER le 01/02/2023 01:00";
    verify(sesService, times(1))
        .sendEmail(eq(null), eq(null), eq(prospectUpdatedHtmlSubject), any(), anyList());
  }

  private static Prospect ignoreHistoryUpdatedOf(Prospect prospect) {
    prospect.getStatusHistories().forEach(history -> history.setUpdatedAt(null));
    return prospect;
  }

  private static Customer ignoreDatesOf(Customer customer) {
    customer.setUpdatedAt(null);
    customer.setCreatedAt(null);
    return null;
  }
}
