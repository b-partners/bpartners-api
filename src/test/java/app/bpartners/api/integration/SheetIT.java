package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.ProspectingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AntiHarmRules;
import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.EventDateRanges;
import app.bpartners.api.endpoint.rest.model.EventEvaluationRules;
import app.bpartners.api.endpoint.rest.model.ImportProspect;
import app.bpartners.api.endpoint.rest.model.InterventionType;
import app.bpartners.api.endpoint.rest.model.ProfessionType;
import app.bpartners.api.endpoint.rest.model.Prospect;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobDetails;
import app.bpartners.api.endpoint.rest.model.PutEventProspectConversion;
import app.bpartners.api.endpoint.rest.model.PutProspectEvaluationJob;
import app.bpartners.api.endpoint.rest.model.RatingProperties;
import app.bpartners.api.endpoint.rest.model.SheetProperties;
import app.bpartners.api.endpoint.rest.model.SheetRange;
import app.bpartners.api.endpoint.rest.security.model.Role;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.SheetEnvContextInitializer;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.BusinessActivityRepository;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.expressif.ExpressifApi;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.google.calendar.CalendarApi;
import app.bpartners.api.repository.google.calendar.drive.DriveApi;
import app.bpartners.api.repository.google.sheets.SheetApi;
import app.bpartners.api.repository.google.sheets.SheetConf;
import app.bpartners.api.repository.jpa.SheetStoredCredentialJpaRep;
import app.bpartners.api.service.CustomerService;
import app.bpartners.api.service.ProspectService;
import app.bpartners.api.service.TransactionService;
import app.bpartners.api.service.UserService;
import app.bpartners.api.service.utils.DateUtils;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.ExtendedValue;
import com.google.api.services.sheets.v4.model.GridData;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.SerializationFeature;

import static app.bpartners.api.integration.ProspectEvaluationIT.geoPosZero;
import static app.bpartners.api.integration.ProspectEvaluationIT.prospectRatingResult;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_HOLDER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_USER_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.joeDoeAccountHolder;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.repository.google.calendar.CalendarApi.dateTimeFrom;
import static app.bpartners.api.repository.implementation.ProspectRepositoryImpl.ANTI_HARM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = SheetEnvContextInitializer.class)
@AutoConfigureMockMvc
@Slf4j
// /!\ Important ! Run only in local
public class SheetIT extends MockedThirdParties {
  private static final String JSON_MIME_TYPE = "application/json";
  public static final String TEST_SHEET_ID = "1JBSbBGawokv7gOR_B1_MMvORYOJQlHroOXj06T3tSYY";
  public static final String GOLDEN_SOURCE_SHEET_ID =
      "1zLlb1m0vlS5Qn1T2sGI1q9BWzkXkaHHnjVbMfOVVuZ0";
  public static final String GOLDEN_SOURCE_EXCEL_ID = "1KHJnf1ONumV3EeOwaKHgzPBYURSnEZ4R";

  public static final String GOLDEN_SOURCE_EXCEL_NAME =
      "Golden source Depa1 Depa 2 - Prospect métier Antinuisibles  Serrurier .xlsx";
  public static final String GOLDEN_SOURCE_SPR_SHEET_NAME =
      "Golden source Depa1 Depa 2 - Prospect métier Antinuisibles  Serrurier ";

  public static final String TEST_SPR_SHEET_NAME = "Test";
  public static final String GOLDEN_SOURCE_SHEET_NAME = "Local Ryan";
  public static final String PROFESSION = "DEPANNEUR";
  public static final String CAL1_ID = "";
  @Autowired
  private SheetApi sheetApi;
  @Autowired
  private SheetConf sheetConf;
  @Autowired
  private DriveApi driveApi;
  @Autowired
  private SheetStoredCredentialJpaRep storeRepository;
  @MockBean
  private TransactionService transactionService;
  @MockBean
  private BanApi banApiMock;
  @MockBean
  private CustomerService customerService;
  @MockBean
  private CalendarApi calendarApiMock;
  @Autowired
  private ProspectService prospectService;
  @MockBean
  private ExpressifApi expressifApiMock;
  @Autowired
  private ProspectRepository prospectRepository;
  @Autowired
  private BusinessActivityRepository businessRepository;
  @Autowired
  private UserService userService;

  private static PutEventProspectConversion prospectEvent1() {
    return new PutEventProspectConversion()
        .calendarId(CAL1_ID)
        .evaluationRules(new EventEvaluationRules()
            .antiHarmRules(new AntiHarmRules()
                .infestationType("souris")
                .interventionTypes(List.of(InterventionType.DISINFECTION)))
            .profession(ProfessionType.ANTI_HARM))
        .eventDateRanges(new EventDateRanges()
            .from(eventDateMin())
            .to(eventDateMax()))
        .ratingProperties(
            new RatingProperties()
                .minCustomerRating(5.0)
                .minProspectRating(5.0));
  }

  private static PutEventProspectConversion prospectEvent2() {
    return prospectEvent1();
  }

  private static Instant eventDateMax() {
    return Instant.parse("2023-09-01T00:00:00.00Z");
  }

  private static Instant eventDateMin() {
    return Instant.parse("2023-09-30T23:59:59.00Z");
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);

    when(banApiMock.search(any())).thenReturn(geoPosZero());
    when(banApiMock.fSearch(any())).thenReturn(geoPosZero());
    User user = userService.getUserById(JOE_DOE_ID);
    userService.saveUser(user.toBuilder()
        .roles(List.of(Role.EVAL_PROSPECT))
        .build());
  }

  private static ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN,
        SheetEnvContextInitializer.getHttpServerPort());
  }

  @Test
  void import_prospects_through_sheet_ok() throws ApiException {
    User user = userService.getUserById(JOE_DOE_ID);
    User savedUser = userService.saveUser(user.toBuilder()
        .roles(List.of(Role.EVAL_PROSPECT))
        .build());
    businessRepository.save(BusinessActivity.builder()
        .accountHolder(joeDoeAccountHolder())
        .primaryActivity(ANTI_HARM)
        .secondaryActivity(null)
        .build());
    when(expressifApiMock.process(any())).thenReturn(List.of(prospectRatingResult()));
    when(banApiMock.fSearch(any())).thenReturn(geoPosZero());
    int minRange = 2;
    int maxRange = 5;
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    List<Prospect> actual = api.importProspects(JOE_DOE_ACCOUNT_HOLDER_ID, new ImportProspect()
        .spreadsheetImport(new SheetProperties()
            .spreadsheetName(SheetIT.GOLDEN_SOURCE_SPR_SHEET_NAME)
            .sheetName(SheetIT.GOLDEN_SOURCE_SHEET_NAME)
            .ranges(new SheetRange()
                .min(minRange)
                .max(maxRange))));

    assertEquals(4, actual.size());
  }

  @Test
  void convert_events_to_prospect_ok() throws ApiException {
    User user = userService.getUserById(JOE_DOE_ID);
    User savedUser = userService.saveUser(user.toBuilder()
        .roles(List.of(Role.EVAL_PROSPECT))
        .build());
    businessRepository.save(BusinessActivity.builder()
        .accountHolder(joeDoeAccountHolder())
        .primaryActivity(ANTI_HARM)
        .secondaryActivity(null)
        .build());
    when(expressifApiMock.process(any())).thenReturn(List.of(prospectRatingResult()));
    when(banApiMock.fSearch(any())).thenReturn(geoPosZero());
    when(calendarApiMock.getEvents(JOE_DOE_ID, CAL1_ID, dateTimeFrom(eventDateMin()),
        dateTimeFrom(eventDateMax())))
        .thenReturn(
            List.of(
                new Event().setLocation("Location1"),
                new Event().setLocation("Location2")));
    ApiClient joeDoeClient = anApiClient();
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    List<ProspectEvaluationJobDetails> actual1 =
        api.runProspectEvaluationJobs(JOE_DOE_USER_ID, List.of(
            new PutProspectEvaluationJob()
                .eventProspectConversion(prospectEvent1())));
    List<ProspectEvaluationJobDetails> actual2 =
        api.runProspectEvaluationJobs(JOE_DOE_USER_ID,
            List.of(
                new PutProspectEvaluationJob()
                    .eventProspectConversion(prospectEvent2())));

    assertEquals(List.of(), actual1);
    assertEquals(List.of(), actual2);
  }

  private static ProspectEvalInfo prospectEvalInfo1() {
    return ProspectEvalInfo.builder()
        .owner("3d0fbbc4-d0cf-4b86-8d80-8f86165e56dd")
        .name("Biscuits")
        .website("https://biscuit-madeleine-cooky.fr/")
        .address("1 Rue des Pâtissiers, 60200 Compiègne, France")
        .phoneNumber("33 3 60 40 54 21 /03 60 40 54 21")
        .email("contact@biscuit-madeleine-cooky.fr")
        .managerName("Khoukha AOUICI DIT AOUICHAT ")
        .mailSent(null)
        .postalCode("60200")
        .city("Compiègne")
        .companyCreationDate(DateUtils.from_dd_MM_YYYY("01/01/2023"))
        .category("Restaurant")
        .subcategory("Magasin de gâteaux")
        .contactNature(ProspectEvalInfo.ContactNature.PROSPECT)
        .reference(null)
        .coordinates(null)
        .build();
  }

  @Test
  void read_prospects_filtered_from_sheet_ok() {
    List<ProspectEvalInfo> prospectEvalInfos = prospectService.readFromSheets(
        JOE_DOE_ID,
        GOLDEN_SOURCE_SPR_SHEET_NAME,
        GOLDEN_SOURCE_SHEET_NAME,
        "3d0fbbc4-d0cf-4b86-8d80-8f86165e56dd");

    assertEquals(1, prospectEvalInfos.size());
    assertEquals(prospectEvalInfos.get(0), prospectEvalInfo1());
  }

  @Test
  void read_prospects_eval_from_sheet_ok() {
    int minRange = 2;
    int maxRange = 4;
    List<ProspectEval> actual = prospectService.readEvaluationsFromSheets(
        JOE_DOE_ID,
        GOLDEN_SOURCE_SPR_SHEET_NAME,
        GOLDEN_SOURCE_SHEET_NAME,
        minRange, maxRange);

    assertEquals(3, actual.size());
    //TODO: verify attributes of each eval
  }


  @Test
  void read_prospects_info_from_sheet_ok() {
    List<ProspectEvalInfo> actual = prospectService.readFromSheets(
        JOE_DOE_ID,
        GOLDEN_SOURCE_SPR_SHEET_NAME,
        GOLDEN_SOURCE_SHEET_NAME);

    assertEquals(3, actual.size());
    assertTrue(actual.contains(prospectEvalInfo1()));
  }

  @Test
  void read_all_excel_file_ok() {
    Credential loadedCredentials = sheetConf.getLocalCredentials(JOE_DOE_ID);

    FileList actual = driveApi.getSpreadSheets(loadedCredentials);

    List<File> files = actual.getFiles();
    List<String> fileNames = files.stream()
        .map(File::getName)
        .collect(Collectors.toList());
    assertEquals(3, files.size());
    assertEquals(
        List.of(GOLDEN_SOURCE_SPR_SHEET_NAME, GOLDEN_SOURCE_EXCEL_NAME, TEST_SPR_SHEET_NAME),
        fileNames);
  }

  @Test
  void write_sheets_from_local_credentials_ok() throws IOException {
    Credential localCredential = sheetConf.getLocalCredentials(JOE_DOE_ID);
    Sheets sheetsService = sheetApi.initService(sheetConf, localCredential);

    /*GridData gridData = new GridData()
        .setRowData(Collections.singletonList(
            new RowData().setValues(Collections.singletonList(
                new CellData().setUserEnteredValue(
                    new ExtendedValue().setStringValue("Nouvelle valeur"))
            ))
        ));*/

    CellData cellData = new CellData()
        .setUserEnteredValue(
            new ExtendedValue()
                .setStringValue("Nouvelle valeur")
        );
    UpdateCellsRequest updateRequest = new UpdateCellsRequest()
        .setFields("userEnteredValue")
        .setRange(new GridRange().setStartRowIndex(0).setStartColumnIndex(0))
        .setRows(Collections.singletonList(new RowData()
            .setValues(
                Collections.singletonList(cellData)
            )));

    Request updateCellRequest = new Request().setUpdateCells(updateRequest);

    BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest()
        .setRequests(Collections.singletonList(updateCellRequest));

    sheetsService.spreadsheets()
        .batchUpdate(TEST_SHEET_ID, batchUpdateRequest)
        .execute();
  }

  @Test
  void read_sheets_from_local_credentials_ok() {
    Credential localCredentials = sheetConf.getLocalCredentials(JOE_DOE_ID);

    int minRange = 2;
    int maxRange = 100;
    Spreadsheet spreadsheet =
        sheetApi.getSpreadsheet(
            GOLDEN_SOURCE_SHEET_ID, GOLDEN_SOURCE_SHEET_NAME, minRange, maxRange, localCredentials);
    List<Sheet> sheets = spreadsheet.getSheets();
    String firstValue = null;
    for (Sheet s : sheets) {
      List<GridData> gridData = s.getData();
      GridData gridData1 = gridData.get(0);
      List<RowData> rowData = gridData1.getRowData();
      RowData rowData1 = rowData.get(0);
      List<CellData> cellData = rowData1.getValues();
      firstValue = cellData.get(0).getFormattedValue();
    }
    assertNotNull(spreadsheet);
    assertNotNull(firstValue);
    log.info("First value {}", firstValue);
  }

  @SneakyThrows
  private static void downloadSheets(List<Spreadsheet> spreadsheets) {
    ObjectMapper om = new ObjectMapper();
    om.enable(SerializationFeature.INDENT_OUTPUT);
    om.writeValue(new java.io.File("spreadsheets.json"), spreadsheets);
  }

  @SneakyThrows
  private static void downloadProspect(List<EvaluatedProspect> prospects) {
    ObjectMapper om = new ObjectMapper();
    om.enable(SerializationFeature.INDENT_OUTPUT);
    om.writeValue(new java.io.File("prospects.json"), prospects);
  }
}
