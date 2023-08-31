package app.bpartners.api.integration;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.SheetEnvContextInitializer;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.google.calendar.drive.DriveApi;
import app.bpartners.api.repository.google.sheets.SheetApi;
import app.bpartners.api.repository.google.sheets.SheetConf;
import app.bpartners.api.repository.jpa.SheetStoredCredentialJpaRep;
import app.bpartners.api.repository.jpa.model.HSheetStoredCredential;
import app.bpartners.api.service.CustomerService;
import app.bpartners.api.service.TransactionService;
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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.SerializationFeature;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = SheetEnvContextInitializer.class)
@AutoConfigureMockMvc
@Slf4j
// /!\ Important ! Run only in local
public class SheetIT extends MockedThirdParties {
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
  private BanApi banApi;
  @MockBean
  private CustomerService customerService;

  @Test
  void read_all_excel_file_ok() {
    Credential loadedCredentials = sheetConf.getLocalCredentials(JOE_DOE_ID);

    FileList actual = driveApi.getSpreadSheets(loadedCredentials);

    List<File> files = actual.getFiles();
    List<String> fileNames = files.stream()
        .map(File::getName)
        .collect(Collectors.toList());
    log.info(files.toString());
    assertEquals(2, files.size());
    assertEquals(
        List.of(
            "Golden source Depa1 Depa 2 - Prospect métier Antinuisibles  Serrurier .xlsx",
            "Test"),
        fileNames);
  }

  @Test
  void write_sheets_from_local_credentials_ok() throws IOException {
    Credential localCredential = sheetConf.getLocalCredentials(JOE_DOE_ID);
    Sheets sheetsService = sheetApi.initService(sheetConf, localCredential);

    String idSheet = "1JBSbBGawokv7gOR_B1_MMvORYOJQlHroOXj06T3tSYY";
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
        .batchUpdate(idSheet, batchUpdateRequest)
        .execute();
  }

  @Test
  void read_sheets_from_local_credentials_ok() {
    String idSheet = "1JBSbBGawokv7gOR_B1_MMvORYOJQlHroOXj06T3tSYY";
    Credential loadedCredentials = sheetConf.getLocalCredentials(JOE_DOE_ID);

    Spreadsheet sheet = sheetApi.getSheet(idSheet, loadedCredentials);
    List<Sheet> sheets = sheet.getSheets();
    String firstValue = null;
    for (Sheet s : sheets) {
      List<GridData> gridData = s.getData();
      GridData gridData1 = gridData.get(0);
      List<RowData> rowData = gridData1.getRowData();
      RowData rowData1 = rowData.get(0);
      List<CellData> cellData = rowData1.getValues();
      firstValue = cellData.get(0).getFormattedValue();
    }
    assertNotNull(sheet);
    assertEquals("Nouvelle valeur", firstValue);
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
