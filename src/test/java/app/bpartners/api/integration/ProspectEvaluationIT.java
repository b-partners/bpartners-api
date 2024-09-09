package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.JobStatusValue.FAILED;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.FINISHED;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.IN_PROGRESS;
import static app.bpartners.api.endpoint.rest.model.JobStatusValue.NOT_STARTED;
import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static app.bpartners.api.endpoint.rest.validator.ProspectRestValidator.XLSX_FILE;
import static app.bpartners.api.integration.conf.utils.TestUtils.BEARER_PREFIX;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_HOLDER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.geoPosZero;
import static app.bpartners.api.integration.conf.utils.TestUtils.joeDoeAccountHolder;
import static app.bpartners.api.integration.conf.utils.TestUtils.prospect1;
import static app.bpartners.api.integration.conf.utils.TestUtils.prospect2;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.customerType;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.infestationType;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.interventionType;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.professionalCustomerType;
import static app.bpartners.api.repository.implementation.ProspectRepositoryImpl.ANTI_HARM;
import static app.bpartners.api.service.ProspectService.DEFAULT_RATING_PROSPECT_TO_CONVERT;
import static app.bpartners.api.service.ProspectService.defaultStatusHistory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.ProspectingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Area;
import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.InterventionResult;
import app.bpartners.api.endpoint.rest.model.NewInterventionOption;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobDetails;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobInfo;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobStatus;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobType;
import app.bpartners.api.endpoint.rest.security.model.Role;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.User;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.BusinessActivityRepository;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.expressif.ExpressifApi;
import app.bpartners.api.repository.expressif.model.OutputValue;
import app.bpartners.api.service.CustomerService;
import app.bpartners.api.service.ProspectService;
import app.bpartners.api.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@Slf4j
class ProspectEvaluationIT extends MockedThirdParties {
  public static final String PROSPECT_EVAL_JOB1_ID = "pe_job_id1";
  @MockBean private BanApi banApiMock;
  @MockBean private ExpressifApi expressifApiMock;
  @Autowired private ProspectRepository prospectRepository;
  @Autowired private ProspectService prospectService;
  @Autowired private BusinessActivityRepository businessRepository;
  @Autowired private CustomerService customerService;
  @Autowired private UserService userService;
  public static int PAGE = 1;
  public static int PAGESIZE = 10;

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  public static OutputValue<Object> prospectRatingResult() {
    return OutputValue.builder().name("Notation du prospect").value(10.0).build();
  }

  private static OutputValue<Object> customerRatingResult() {
    return OutputValue.builder().name("Notation de l'ancien client").value(8.0).build();
  }

  private static Geojson defaultGeoJson() {
    return new Geojson().type("Point").latitude(0.0).longitude(0.0);
  }

  private static ProspectEvaluationJobDetails evalDetails1() {
    return new ProspectEvaluationJobDetails()
        .id("pe_job_id1")
        .status(new ProspectEvaluationJobStatus().value(NOT_STARTED).message(null))
        .type(ProspectEvaluationJobType.CALENDAR_EVENT_CONVERSION)
        .startedAt(Instant.parse("2022-01-01T00:00:00.00Z"))
        .endedAt(null)
        .results(List.of(prospect1(), prospect2()));
  }

  private static ProspectEvaluationJobInfo evalJob1() {
    return new ProspectEvaluationJobInfo()
        .id("pe_job_id1")
        .status(new ProspectEvaluationJobStatus().value(NOT_STARTED).message(null))
        .type(ProspectEvaluationJobType.CALENDAR_EVENT_CONVERSION)
        .startedAt(Instant.parse("2022-01-01T00:00:00.00Z"))
        .metadata(new HashMap<>())
        .endedAt(null);
  }

  private static ProspectEvaluationJobInfo evalJob3() {
    return new ProspectEvaluationJobInfo()
        .id("pe_job_id3")
        .status(new ProspectEvaluationJobStatus().value(FAILED).message("Connection reset"))
        .type(ProspectEvaluationJobType.ADDRESS_CONVERSION)
        .startedAt(Instant.parse("2022-01-03T01:00:00.00Z"))
        .metadata(new HashMap<>())
        .endedAt(Instant.parse("2022-01-03T02:00:00.00Z"));
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);

    when(banApiMock.search(any())).thenReturn(geoPosZero());
    when(banApiMock.fSearch(any())).thenReturn(geoPosZero());
    User user = userService.getUserById(JOE_DOE_ID);
    userService.save(user.toBuilder().roles(List.of(Role.EVAL_PROSPECT)).build());
  }

  /*
  TODO: to complete with uploadFile with custom Accept value, application/pdf for example
  @Test
   void evaluate_prospects_bad_headers_ko() throws IOException, InterruptedException {
     File prospectFile = new ClassPathResource("files/prospect-ok.xlsx").getFile();
    /!\  null value seems not accepted by HttpHeader class
     assertThrowsBadRequestException("",
         () -> uploadFile(JOE_DOE_ACCOUNT_HOLDER_ID, prospectFile, null));
     var actual = uploadFileJson(JOE_DOE_ACCOUNT_HOLDER_ID, prospectFile);
     assertEquals(500, actual.statusCode());
   } */
  @Test
  void read_prospect_evaluation_jobs_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    var actual = api.getProspectEvaluationJobs(JOE_DOE_ACCOUNT_HOLDER_ID, null);
    var actualNotStarted =
        api.getProspectEvaluationJobs(JOE_DOE_ACCOUNT_HOLDER_ID, List.of(NOT_STARTED));
    var actualFailed = api.getProspectEvaluationJobs(JOE_DOE_ACCOUNT_HOLDER_ID, List.of(FAILED));
    var actualFinished =
        api.getProspectEvaluationJobs(JOE_DOE_ACCOUNT_HOLDER_ID, List.of(FINISHED));
    var actualInProgress =
        api.getProspectEvaluationJobs(JOE_DOE_ACCOUNT_HOLDER_ID, List.of(IN_PROGRESS));
    var actualFinishedAndFailed =
        api.getProspectEvaluationJobs(JOE_DOE_ACCOUNT_HOLDER_ID, List.of(FINISHED, FAILED));

    assertEquals(5, actual.size());
    assertEquals(1, actualNotStarted.size());
    assertEquals(1, actualFailed.size());
    assertTrue(
        actualNotStarted.stream()
            .allMatch(
                jobInfo ->
                    jobInfo.getStatus() != null && jobInfo.getStatus().getValue() == NOT_STARTED));
    assertTrue(
        actualFailed.stream()
            .allMatch(
                jobInfo ->
                    jobInfo.getStatus() != null && jobInfo.getStatus().getValue() == FAILED));
    assertTrue(
        actualFinished.stream()
            .allMatch(
                jobInfo ->
                    jobInfo.getStatus() != null && jobInfo.getStatus().getValue() == FINISHED));
    assertTrue(
        actualInProgress.stream()
            .allMatch(
                jobInfo ->
                    jobInfo.getStatus() != null && jobInfo.getStatus().getValue() == IN_PROGRESS));
    assertTrue(
        actualNotStarted.stream()
            .allMatch(
                jobInfo ->
                    jobInfo.getStatus() != null && jobInfo.getStatus().getValue() == NOT_STARTED));
    assertTrue(
        actualFinishedAndFailed.containsAll(
            Stream.of(actualFailed, actualFinished)
                .flatMap(List::stream)
                .collect(Collectors.toList())));
    assertTrue(
        actual.containsAll(
            Stream.of(actualNotStarted, actualFailed, actualFinished, actualInProgress)
                .flatMap(List::stream)
                .collect(Collectors.toList())));
    assertEquals(evalJob1(), actualNotStarted.get(0));
    assertEquals(evalJob3(), actualFailed.get(0));
  }

  /*
  TODO: make test pass
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
  @Test
  void read_prospect_evaluation_job_details_ok() throws ApiException {
    Prospect prospect1 = prospectService.getById(prospect1().getId());
    Prospect prospect2 = prospectService.getById(prospect2().getId());
    prospectService.saveAllWithoutSogefi(List.of(
        prospect1.toBuilder()
            .idJob(PROSPECT_EVAL_JOB1_ID)
            .build(),
        prospect2.toBuilder()
            .idJob(PROSPECT_EVAL_JOB1_ID)
            .build()));
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    var actual =
        api.getProspectEvaluationJobDetailsById(JOE_DOE_ACCOUNT_HOLDER_ID, PROSPECT_EVAL_JOB1_ID);

    assertEquals(evalDetails1(), actual);
  }*/

  @Test
  @Disabled
  // TODO: check how it works and correct assertions
  void evaluate_prospects_ok() throws IOException, InterruptedException {
    businessRepository.save(
        BusinessActivity.builder()
            .accountHolder(joeDoeAccountHolder())
            .primaryActivity(ANTI_HARM)
            .secondaryActivity(null)
            .build());
    when(expressifApiMock.process(any())).thenReturn(List.of(prospectRatingResult()));
    when(banApiMock.fSearch(any())).thenReturn(geoPosZero());

    Resource prospectFile = new ClassPathResource("files/prospect-ok.xlsx");
    HttpResponse<String> jsonResponse =
        uploadFileJson(JOE_DOE_ACCOUNT_HOLDER_ID, prospectFile.getFile(), null);
    HttpResponse<byte[]> excelResponse =
        uploadFileExcel(JOE_DOE_ACCOUNT_HOLDER_ID, prospectFile.getFile(), null);

    List<EvaluatedProspect> actualJson =
        new ObjectMapper()
            .findAndRegisterModules()
            .readValue(jsonResponse.body(), new TypeReference<>() {});

    List<Prospect> fromEvaluated = convertRestToDomain(actualJson);
    List<Prospect> actualProspect = getPersistedProspect();
    assertEquals(HttpStatus.OK.value(), excelResponse.statusCode());
    assertEquals(5, actualJson.size());
    assertEquals(
        expectedProspectEval1(actualJson).reference(actualJson.get(0).getReference()),
        actualJson.get(0));
    // assertEquals((fromEvaluated), actualProspect);
    assertEquals(List.of(fromEvaluated), actualProspect);
    assertTrue(actualProspect.containsAll(fromEvaluated));

    /*
    /!\ Uncomment only for local test use case
    var actualExcel = excelResponse.body();
    File generatedFile = new File("prospects-" + randomUUID() + ".xlsx");
    OutputStream os = new FileOutputStream(generatedFile);
    os.write(actualExcel);
    os.close();
    */
  }

  private List<Prospect> getPersistedProspect() {
    return prospectRepository
        .findAllByIdAccountHolder(JOE_DOE_ACCOUNT_HOLDER_ID, "", null, TO_CONTACT, PAGE, PAGESIZE)
        .stream()
        .peek(prospect -> prospect.setId(null))
        .collect(Collectors.toList());
  }

  private static List<Prospect> convertRestToDomain(List<EvaluatedProspect> actualJson) {
    return actualJson.stream()
        .filter(hasRatingOverDefault())
        .map(ProspectEvaluationIT::convertToDomain)
        .collect(Collectors.toList());
  }

  @Test
  @Disabled
  // TODO: check how it works and correct assertions
  void evaluate_prospects_and_old_customers_ok() throws IOException, InterruptedException {
    businessRepository.save(
        BusinessActivity.builder()
            .accountHolder(joeDoeAccountHolder())
            .primaryActivity(ANTI_HARM)
            .secondaryActivity(null)
            .build());
    when(expressifApiMock.process(any()))
        .thenReturn(List.of(prospectRatingResult(), customerRatingResult()));
    when(banApiMock.search(any())).thenReturn(geoPosZero());
    when(banApiMock.fSearch(any())).thenReturn(geoPosZero());

    Resource prospectFile = new ClassPathResource("files/prospect-ok.xlsx");
    HttpResponse<String> jsonResponse =
        uploadFileJson(
            JOE_DOE_ACCOUNT_HOLDER_ID, prospectFile.getFile(), NewInterventionOption.ALL);

    List<EvaluatedProspect> actualJson =
        new ObjectMapper()
            .findAndRegisterModules()
            .readValue(jsonResponse.body(), new TypeReference<>() {});

    List<Prospect> fromEvaluated = convertRestToDomain(actualJson);
    List<Prospect> actualProspect = getPersistedProspect();
    int customersSize = customerService.findByAccountHolderId(JOE_DOE_ACCOUNT_HOLDER_ID).size();
    int evaluatedInterventionSize = 5 * customersSize;
    int evaluatedRobberySize = 1; // TODO: remove when handling robbery fact correctly
    assertEquals(evaluatedInterventionSize + evaluatedRobberySize, actualJson.size());
    // TODO: make assertions pass correctly !
    // assertTrue(actualProspect.containsAll(fromEvaluated));
  }

  @Test
  void evaluate_prospects_ko() throws IOException, InterruptedException {
    businessRepository.save(
        BusinessActivity.builder()
            .accountHolder(joeDoeAccountHolder())
            .primaryActivity(ANTI_HARM)
            .secondaryActivity(null)
            .build());
    when(expressifApiMock.process(any())).thenReturn(List.of(prospectRatingResult()));
    Resource prospectFile = new ClassPathResource("files/prospect-ko-400.xlsx");
    HttpResponse<String> jsonResponse =
        uploadFileJson(JOE_DOE_ACCOUNT_HOLDER_ID, prospectFile.getFile(), null);

    assertEquals(
        "{\"type\":\"400 BAD_REQUEST\",\"message\":\""
            + "Row-3,Cell-17 accepts only `Yes` or `No` but was Other. "
            + "Row-3,Cell-21 only support these values "
            + Arrays.toString(interventionType())
            + " but was Other intervention. "
            + "Row-3,Cell-22 only support these values "
            + Arrays.toString(infestationType())
            + " but was otherinfestation. Row-3,Cell-24 only support these values "
            + Arrays.toString(customerType())
            + " but was Other infestation."
            + " Row-3,Cell-25 only support these values "
            + Arrays.toString(professionalCustomerType())
            + " but was Other professionnal. \"}",
        jsonResponse.body());
  }

  private static Predicate<EvaluatedProspect> hasRatingOverDefault() {
    return evaluated ->
        evaluated.getInterventionResult() != null
            && evaluated.getInterventionResult().getValue().doubleValue()
                >= DEFAULT_RATING_PROSPECT_TO_CONVERT;
  }

  private static Prospect convertToDomain(EvaluatedProspect evaluated) {
    return Prospect.builder()
        .idHolderOwner(JOE_DOE_ACCOUNT_HOLDER_ID)
        .name(evaluated.getName())
        .email(evaluated.getEmail())
        .phone(evaluated.getPhone())
        .address(evaluated.getAddress())
        .statusHistories(defaultStatusHistory())
        .townCode(evaluated.getTownCode())
        .location(
            new Geojson()
                .latitude(evaluated.getArea().getGeojson().getLatitude())
                .longitude(evaluated.getArea().getGeojson().getLongitude()))
        .rating(
            Prospect.ProspectRating.builder()
                .value(evaluated.getInterventionResult().getValue().doubleValue())
                .lastEvaluationDate(evaluated.getEvaluationDate())
                .build())
        .build();
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
                .value(BigDecimal.valueOf((Double) prospectRatingResult().getValue())));
  }

  private HttpResponse<String> uploadFileJson(
      String accountHolderId, File toUpload, NewInterventionOption interventionOption)
      throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    basePath
                        + "/accountHolders/"
                        + accountHolderId
                        + "/prospects"
                        + "/prospectsEvaluation"))
            .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .method("POST", HttpRequest.BodyPublishers.ofFile(toUpload.toPath()));

    if (interventionOption != null) {
      requestBuilder.header("newInterventionOption", String.valueOf(interventionOption));
    }

    HttpResponse<String> response =
        unauthenticatedClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

    return response;
  }

  private HttpResponse<byte[]> uploadFileExcel(
      String accountHolderId, File toUpload, NewInterventionOption interventionOption)
      throws IOException, InterruptedException {
    HttpClient unauthenticatedClient = HttpClient.newBuilder().build();
    String basePath = "http://localhost:" + localPort;

    HttpRequest.Builder requestBuilder =
        HttpRequest.newBuilder()
            .uri(
                URI.create(
                    basePath
                        + "/accountHolders/"
                        + accountHolderId
                        + "/prospects"
                        + "/prospectsEvaluation"))
            .header("Authorization", BEARER_PREFIX + JOE_DOE_TOKEN)
            .header(HttpHeaders.ACCEPT, XLSX_FILE)
            .method("POST", HttpRequest.BodyPublishers.ofFile(toUpload.toPath()));

    if (interventionOption != null) {
      requestBuilder.header("newInterventionOption", String.valueOf(interventionOption));
    }

    return unauthenticatedClient.send(
        requestBuilder.build(), HttpResponse.BodyHandlers.ofByteArray());
  }
}
