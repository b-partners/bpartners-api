package app.bpartners.api.repository.google.sheets;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.google.calendar.CalendarConf.JSON_FACTORY;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.repository.google.calendar.drive.DriveApi;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.AddFilterViewRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BooleanCondition;
import com.google.api.services.sheets.v4.model.ConditionValue;
import com.google.api.services.sheets.v4.model.FilterCriteria;
import com.google.api.services.sheets.v4.model.FilterSpec;
import com.google.api.services.sheets.v4.model.FilterView;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Data
@Component
@AllArgsConstructor
public class SheetApi {
  public static final String DEFAULT_CALENDAR = "primary";
  public static final String START_TIME_ATTRIBUTE = "startTime";
  private final DriveApi driveApi;
  private final SheetConf sheetsConf;

  @SneakyThrows
  public Sheets initService(SheetConf sheetsConf, Credential credential) {
    NetHttpTransport trustedTransport = sheetsConf.getTrustedTransport();
    String applicationName = sheetsConf.getApplicationName();
    return new Sheets.Builder(trustedTransport, JSON_FACTORY, credential)
        .setApplicationName(applicationName)
        .build();
  }

  public Spreadsheet getSpreadsheetByNames(
      String idUser, String spreadsheetName, String sheetName, Integer minRange, Integer maxRange) {
    File spreadSheetFile = driveApi.getFileByIdUserAndName(idUser, spreadsheetName);
    String spreadSheetFileId = spreadSheetFile.getId();
    return getSpreadsheet(idUser, spreadSheetFileId, sheetName, minRange, maxRange);
  }

  public Spreadsheet createFilterView(String idUser, String idSpreadsheet, String artisanOwner) {
    return createFilterView(idSpreadsheet, artisanOwner, sheetsConf.loadCredential(idUser));
  }

  public Spreadsheet createFilterView(
      String idSpreadsheet, String artisanOwner, Credential credential) {
    Sheets sheetsService = initService(sheetsConf, credential);
    try {
      FilterCriteria filterCriteria =
          new FilterCriteria()
              .setHiddenValues(new ArrayList<>())
              .setCondition(
                  new BooleanCondition()
                      .setType("TEXT_CONTAINS")
                      .setValues(List.of(new ConditionValue().setUserEnteredValue(artisanOwner))));

      FilterView filterView =
          new FilterView()
              .setFilterViewId(1)
              .setTitle("Artisan owner filter")
              .setRange(
                  new GridRange()
                      .setSheetId(821911047)
                      .setStartRowIndex(1)
                      .setEndColumnIndex(10000)
                      .setStartColumnIndex(0)
                      .setEndColumnIndex(12))
              .setFilterSpecs(
                  List.of(new FilterSpec().setFilterCriteria(filterCriteria).setColumnIndex(9)));

      BatchUpdateSpreadsheetRequest batchUpdateRequest =
          new BatchUpdateSpreadsheetRequest()
              .setRequests(
                  List.of(
                      new Request()
                          .setAddFilterView(new AddFilterViewRequest().setFilter(filterView))));
      var response =
          sheetsService.spreadsheets().batchUpdate(idSpreadsheet, batchUpdateRequest).execute();
      return response.getUpdatedSpreadsheet();
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        throw new ForbiddenException(
            "Google Calendar Token is expired or invalid. Give your consent again.");
      } else {
        throw new ApiException(SERVER_EXCEPTION, e);
      }
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  @SneakyThrows
  public Spreadsheet getSpreadsheet(
      String idUser, String idSpreadsheet, String sheetName, Integer minRange, Integer maxRange) {
    return getSpreadsheet(
        idSpreadsheet, sheetName, minRange, maxRange, sheetsConf.googleCredential());
  }

  public Spreadsheet getSpreadsheet(
      String idSpreadsheet,
      String sheetName,
      Integer minRange,
      Integer maxRange,
      Credential credential) {
    Sheets sheetsService = initService(sheetsConf, credential);
    Spreadsheet spreadsheet;
    try {
      String range = String.format("'%s'!A%d:AF%d", sheetName, minRange, maxRange);
      spreadsheet =
          sheetsService
              .spreadsheets()
              .get(idSpreadsheet)
              .setRanges(List.of(range))
              .setIncludeGridData(true)
              .execute();
    } catch (GoogleJsonResponseException e) {
      switch (e.getStatusCode()) {
        case 400:
          throw new BadRequestException("[Google Sheet] " + e.getDetails().getMessage());
        case 403:
          throw new ForbiddenException("[Google Sheet] " + e.getDetails().getMessage());
        case 401:
          throw new ForbiddenException(
              "Google Drive/Sheet Token is expired or invalid. Give your consent again.");
        default:
          throw new ApiException(SERVER_EXCEPTION, e);
      }
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return spreadsheet;
  }

  public String initConsent(String callBackUri) {
    return sheetsConf.getOauthRedirectUri(callBackUri);
  }

  public Credential storeCredential(String idUser, String authorizationCode, String redirectUrl) {
    return sheetsConf.storeCredential(idUser, authorizationCode, redirectUrl);
  }
}
