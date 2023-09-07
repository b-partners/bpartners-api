package app.bpartners.api.integration;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.SheetEnvContextInitializer;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.google.calendar.drive.DriveApi;
import app.bpartners.api.repository.google.sheets.SheetApi;
import app.bpartners.api.repository.google.sheets.SheetConf;
import app.bpartners.api.repository.jpa.SheetStoredCredentialJpaRep;
import app.bpartners.api.repository.jpa.model.HSheetStoredCredential;
import app.bpartners.api.service.CustomerService;
import app.bpartners.api.service.ProspectService;
import app.bpartners.api.service.TransactionService;
import app.bpartners.api.service.utils.DateUtils;
import com.google.api.client.auth.oauth2.Credential;
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
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
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
  public static final String TEST_SHEET_ID = "1JBSbBGawokv7gOR_B1_MMvORYOJQlHroOXj06T3tSYY";
  public static final String GOLDEN_SOURCE_SHEET_ID =
      "1zLlb1m0vlS5Qn1T2sGI1q9BWzkXkaHHnjVbMfOVVuZ0";
  public static final String GOLDEN_SOURCE_EXCEL_ID = "1KHJnf1ONumV3EeOwaKHgzPBYURSnEZ4R";

  public static final String GOLDEN_SOURCE_EXCEL_NAME =
      "Golden source Depa1 Depa 2 - Prospect métier Antinuisibles  Serrurier .xlsx";
  public static final String GOLDEN_SOURCE_SPR_SHEET_NAME =
      "Golden source Depa1 Depa 2 - Prospect métier Antinuisibles  Serrurier ";

  public static final String TEST_SPR_SHEET_NAME = "Test";
  public static final String GOLDEN_SOURCE_SHEET_NAME = "Source Import";
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
  @Autowired
  private ProspectService prospectService;

  @BeforeEach
  public void setUp() {
    when(banApiMock.fSearch(any())).thenReturn(geoPosZero());
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
    List<ProspectEval> prospectEvals = prospectService.readEvaluationsFromSheets(
        JOE_DOE_ID,
        GOLDEN_SOURCE_SPR_SHEET_NAME,
        GOLDEN_SOURCE_SHEET_NAME,
        minRange, maxRange);

    assertEquals(3, prospectEvals.size());
    //TODO: verify attributes of each eval
  }


  @Test
  void read_prospects_info_from_sheet_ok() {
    List<ProspectEvalInfo> prospectEvalInfos = prospectService.readFromSheets(
        JOE_DOE_ID,
        GOLDEN_SOURCE_SPR_SHEET_NAME,
        GOLDEN_SOURCE_SHEET_NAME);

    assertEquals(3, prospectEvalInfos.size());
    assertTrue(prospectEvalInfos.contains(prospectEvalInfo1()));
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
  private static void downloadCredentials(List<HSheetStoredCredential> credentials) {
    ObjectMapper om = new ObjectMapper();
    om.enable(SerializationFeature.INDENT_OUTPUT);
    om.writeValue(new java.io.File("credentials.json"), credentials);
  }
}
