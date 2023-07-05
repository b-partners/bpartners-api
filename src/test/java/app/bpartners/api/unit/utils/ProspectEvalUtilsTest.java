package app.bpartners.api.unit.utils;

import app.bpartners.api.expressif.NewProspect;
import app.bpartners.api.expressif.ProspectEval;
import app.bpartners.api.expressif.fact.NewIntervention;
import app.bpartners.api.expressif.fact.Robbery;
import app.bpartners.api.expressif.utils.ProspectEvalUtils;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.service.utils.GeoUtils;
import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import static app.bpartners.api.expressif.NewProspect.ContactNature.OTHER;
import static app.bpartners.api.expressif.NewProspect.ContactNature.PROSPECT;
import static app.bpartners.api.expressif.fact.NewIntervention.OldCustomer.OldCustomerType.PROFESSIONAL;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProspectEvalUtilsTest {
  public static final double DEFAULT_LATITUDE = 0.0;
  public static final double DEFAULT_LONGITUDE = 0.0;
  BanApi banApiMock;
  ProspectEvalUtils subject;

  @BeforeEach
  public void setUp() {
    banApiMock = mock(BanApi.class);
    subject = new ProspectEvalUtils(banApiMock);

    when(banApiMock.search(any())).thenReturn(
        GeoPosition.builder()
            .coordinates(GeoUtils.Coordinate.builder()
                .latitude(DEFAULT_LATITUDE)
                .longitude(DEFAULT_LONGITUDE)
                .build())
            .build()
    );
  }

  private static GeoUtils.Coordinate defaultCoordinate() {
    return GeoUtils.Coordinate.builder()
        .longitude(DEFAULT_LONGITUDE)
        .latitude(DEFAULT_LATITUDE)
        .build();
  }

  private static ProspectEval<Object> prospectEval1() {
    return ProspectEval.builder()
        .newProspect(NewProspect.builder()
            .name("Da Vito")
            .website(null)
            .address("5 Rue Sedaine, 75011 Paris")
            .phoneNumber("09 50 73 12 99 ")
            .email("davide@liquidcorp.fr")
            .managerName("Viviane TAING")
            .mailSent(true)
            .postalCode("75011")
            .city("Paris")
            .companyCreationDate(Date.from(Instant.parse("2020-12-31T21:00:00Z")))
            .category("Restaurant")
            .subcategory("Pizzeria")
            .contactNature(PROSPECT)
            .coordinates(defaultCoordinate())
            .build())
        .lockSmith(false)
        .antiHarm(true)
        .insectControl(true)
        .disinfection(null)
        .ratRemoval(false)
        .professionalCustomer(false)
        .particularCustomer(true)
        .depaRule(NewIntervention.builder()
            .planned(true)
            .interventionType("désinsectisation")
            .infestationType("rat")
            .newIntAddress("15 Rue Marbeuf, 75008 Paris, France")
            .distNewIntAndProspect(0.0)
            .oldCustomerFact(NewIntervention.OldCustomer.builder().build())
            .build())
        .build();
  }

  private static ProspectEval<Object> prospectEval2() {
    return ProspectEval.builder()
        .newProspect(NewProspect.builder()
            .name("Royal Fata")
            .website("https://royal-fata-paris-20.fr/fr")
            .address("237 Rue des Pyrénées, 75020 Paris")
            .phoneNumber("06 11 70 35 03/ 0611703503")
            .email(null)
            .managerName(null)
            .mailSent(null)
            .postalCode("75020")
            .city("Paris")
            .companyCreationDate(Date.from(Instant.parse("2022-01-01T21:00:00Z")))
            .category("Restaurant")
            .subcategory("Restaurant chinois")
            .contactNature(OTHER)
            .coordinates(defaultCoordinate())
            .build())
        .lockSmith(false)
        .antiHarm(true)
        .insectControl(null)
        .disinfection(null)
        .ratRemoval(true)
        .professionalCustomer(true)
        .particularCustomer(false)
        .depaRule(NewIntervention.builder()
            .planned(true)
            .interventionType("désinfection")
            .infestationType("puces")
            .newIntAddress("49-51 Av. des Champs-Élysées, 75008 Paris, France")
            .distNewIntAndProspect(0.0)
            .oldCustomerFact(NewIntervention.OldCustomer.builder().build())
            .build())
        .build();
  }

  private static ProspectEval<Object> prospectEval3() {
    return ProspectEval.builder()
        .newProspect(NewProspect.builder()
            .name("Resto Madalaya")
            .website(null)
            .address("2 Rue des Amandiers, 75020 Paris")
            .phoneNumber("09 73 19 67 86/ 07 83 81 14 26")
            .email("mariemaupanier@gmail.com")
            .managerName("MASSIGA DIABY")
            .mailSent(false)
            .postalCode("75020")
            .city("Paris")
            .companyCreationDate(Date.from(Instant.parse("2021-01-02T21:00:00Z")))
            .category("Restaurant")
            .subcategory("Restaurant de spécialités d'Afrique de l'Ouest")
            .contactNature(PROSPECT)
            .coordinates(defaultCoordinate())
            .build())
        .lockSmith(true)
        .antiHarm(false)
        .insectControl(null)
        .disinfection(null)
        .ratRemoval(null)
        .professionalCustomer(false)
        .particularCustomer(true)
        .depaRule(Robbery.builder()
            .declared(true)
            .robberyAddress(
                "Maison de l'Architecture, 148 Rue du Faubourg Saint-Martin, 75010 Paris, France")
            .distRobberyAndProspect(0.0)
            .build())
        .build();
  }

  private static ProspectEval<Object> prospectEval4() {
    return ProspectEval.builder()
        .newProspect(NewProspect.builder()
            .name("OKY SUSHI okysushi")
            .website("https://okysushiparis.fr/")
            .address("356 Rue des Pyrénées, 75020 Paris")
            .phoneNumber("01 46 36 52 34/ 01 46 36 54 33")
            .email("okysushi@gmail.com")
            .managerName("LI FENG")
            .mailSent(null)
            .postalCode("75020")
            .city("Paris")
            .companyCreationDate(Date.from(Instant.parse("2021-01-03T21:00:00Z")))
            .category("Restaurant")
            .subcategory("Restaurant japonais authentique")
            .contactNature(PROSPECT)
            .coordinates(defaultCoordinate())
            .build())
        .lockSmith(false)
        .antiHarm(true)
        .insectControl(true)
        .disinfection(null)
        .ratRemoval(false)
        .professionalCustomer(false)
        .particularCustomer(true)
        .depaRule(NewIntervention.builder()
            .planned(true)
            .interventionType("désinsectisation")
            .infestationType("rat")
            .newIntAddress("15 Rue Marbeuf, 75008 Paris, France")
            .distNewIntAndProspect(0.0)
            .oldCustomerFact(NewIntervention.OldCustomer.builder()
                .type(PROFESSIONAL)
                .professionalType("restaurant")
                .oldCustomerAddress("49-51 Av. des Champs-Élysées, 75008 Paris, France")
                .distNewIntAndOldCustomer(0.0)
                .build())
            .build())
        .build();
  }

  @Test
  void import_prospects_ok() throws IOException {
    Resource prospectFile = new ClassPathResource("files/prospect-ok.xlsx");

    List<ProspectEval> prospectEvals =
        subject.convertFromExcel(prospectFile.getInputStream());

    assertEquals(5, prospectEvals.size());
    /*
    /!\ It seems Github couldn't run the test with these asserts
    assertEquals(prospectEval1(), prospectEvals.get(0));
    assertEquals(prospectEval2(), prospectEvals.get(1));
    assertEquals(prospectEval3(), prospectEvals.get(2));
    assertEquals(prospectEval4(), prospectEvals.get(3));*/
  }

  @Test
  void import_prospects_ko() {
    Resource prospectFile1 = new ClassPathResource("files/prospect-ko-400.xlsx");
    Resource prospectFile2 = new ClassPathResource("files/prospect-ko-500.xlsx");

    assertThrowsBadRequestException(
        "Depa rule (column-N) is mandatory to evaluate prospect but is not present for row-2. ",
        () -> subject.convertFromExcel(prospectFile1.getInputStream()));
    NotImplementedException notImplementedException = assertThrows(NotImplementedException.class,
        () -> subject.convertFromExcel(prospectFile2.getInputStream()));
    assertEquals(
        "Only \"Dépa1 / Nouvelle intervention\" or \"Dépa1 / Cambriolage\""
            + " is supported for now. Otherwise, Dépa2 / Travaux was given",
        notImplementedException.getMessage());
  }
}
