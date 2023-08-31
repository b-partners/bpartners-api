package app.bpartners.api.repository.google.sheets;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.ForbiddenException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import java.io.IOException;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.google.calendar.CalendarConf.JSON_FACTORY;

@Slf4j
@Data
@Component
@AllArgsConstructor
public class SheetApi {
  public static final String DEFAULT_CALENDAR = "primary";
  public static final String START_TIME_ATTRIBUTE = "startTime";
  private final SheetConf sheetsConf;

  public Sheets initService(SheetConf sheetsConf, Credential credential) {
    NetHttpTransport trustedTransport = sheetsConf.getTrustedTransport();
    String applicationName = sheetsConf.getApplicationName();
    return new Sheets.Builder(trustedTransport, JSON_FACTORY, credential)
        .setApplicationName(applicationName)
        .build();
  }

  public Spreadsheet getSpreadsheet(String idUser, String idSpreadsheet) {
    return getSpreadsheet(idSpreadsheet, sheetsConf.loadCredential(idUser));
  }

  public Spreadsheet getSpreadsheet(String idSpreadsheet, Credential credential) {
    Sheets sheetsService = initService(sheetsConf, credential);
    Spreadsheet spreadsheet;
    try {
      spreadsheet = sheetsService.spreadsheets().get(idSpreadsheet)
          .setRanges(List.of("'Source Import'!A2:M10000", "'Golden Source dépa 1 & 2'!A2:M10000"))
          .setIncludeGridData(true)
          .execute();
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
    return spreadsheet;
  }
}
