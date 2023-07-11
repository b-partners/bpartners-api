package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.event.S3Conf;
import app.bpartners.api.endpoint.rest.model.Area;
import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.InterventionResult;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.LegalFileRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.expressif.ExpressifApi;
import app.bpartners.api.repository.expressif.model.OutputValue;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitApi;
import app.bpartners.api.repository.prospecting.datasource.buildingpermit.BuildingPermitConf;
import app.bpartners.api.repository.sendinblue.SendinblueConf;
import app.bpartners.api.service.PaymentScheduleService;
import app.bpartners.api.service.utils.GeoUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.TestUtils.BEARER_PREFIX;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_HOLDER_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = ProspectEvaluationIT.ContextInitializer.class)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Slf4j
class ProspectEvaluationIT {
  public static final String PROFESSION_RULE = "DEPANNEUR";
  @MockBean
  private PaymentScheduleService paymentScheduleService;
  @MockBean
  private SentryConf sentryConf;
  @MockBean
  private SendinblueConf sendinblueConf;
  @MockBean
  private S3Conf s3Conf;
  @MockBean
  private CognitoComponent cognitoComponentMock;
  @MockBean
  private FintectureConf fintectureConf;
  @MockBean
  private ProjectTokenManager projectTokenManager;
  @MockBean
  private AccountConnectorRepository accountConnectorRepositoryMock;
  @MockBean
  private LegalFileRepository legalFileRepositoryMock;
  @MockBean
  private BuildingPermitApi buildingPermitApiMock;
  @MockBean
  private BuildingPermitConf buildingPermitConfMock;
  @MockBean
  private BridgeApi bridgeApi;
  @MockBean
  private BanApi banApiMock;
  @MockBean
  private ExpressifApi expressifApiMock;

  private static OutputValue<Object> ratingResult() {
    return OutputValue.builder()
        .name("Notation du prospect")
        .value(10.0)
        .build();
  }

  private static Geojson defaultGeoJson() {
    return new Geojson()
        .type("Point")
        .latitude(0.0)
        .longitude(0.0);
  }


  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);

    when(banApiMock.search(any())).thenReturn(GeoPosition.builder()
        .coordinates(GeoUtils.Coordinate.builder()
            .latitude(0.0)
            .longitude(0.0)
            .build())
        .build()
    );
  }

  @Test
  void evaluate_prospects_ok() throws IOException, InterruptedException {
    when(expressifApiMock.process(any())).thenReturn(List.of(ratingResult()));
    Resource prospectFile = new ClassPathResource("files/prospect-ok.xlsx");
    HttpResponse<String> response = uploadFile(JOE_DOE_ACCOUNT_HOLDER_ID, prospectFile.getFile());

    List<EvaluatedProspect> actual = new ObjectMapper().findAndRegisterModules().readValue(
        response.body(), new TypeReference<>() {
        });

    assertEquals(5, actual.size());
    assertEquals(expectedProspectEval1(actual), actual.get(0));
  }

  private static EvaluatedProspect expectedProspectEval1(List<EvaluatedProspect> actual) {
    return new EvaluatedProspect()
        .evaluationDate(actual.get(0).getEvaluationDate())
        .id(actual.get(0).getId())
        .reference(String.valueOf(1))
        .name("Da Vito")
        .email("davide@liquidcorp.fr")
        .phone("09 50 73 12 99 ")
        .address("5 Rue Sedaine, 75011 Paris")
        .townCode(75011)
        .city("Paris")
        .managerName("Viviane TAING")
        .contactNature(ContactNature.PROSPECT)
        .area(new Area().geojson(defaultGeoJson()))
        .interventionResult(
            new InterventionResult()
                .address("15 Rue Marbeuf, 75008 Paris, France")
                .distanceFromProspect(BigDecimal.valueOf(0.0))
                .value(BigDecimal.valueOf((Double) ratingResult().getValue())));
  }

  private HttpResponse<String> uploadFile(String accountHolderId, File toUpload)
      throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + ProspectEvaluationIT.ContextInitializer.SERVER_PORT;

    HttpResponse<String> response = unauthenticatedClient.send(HttpRequest.newBuilder()
            .uri(URI.create(basePath + "/accountHolders/" + accountHolderId + "/prospects"
                + "/prospectsEvaluation"))
            .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
            .method("POST", HttpRequest.BodyPublishers.ofFile(toUpload.toPath())).build(),
        HttpResponse.BodyHandlers.ofString());

    return response;
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
