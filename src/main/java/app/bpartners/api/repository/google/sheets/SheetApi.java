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

  private Sheets initService(SheetConf sheetsConf, Credential credential) {
    NetHttpTransport trustedTransport = sheetsConf.getTrustedTransport();
    String applicationName = sheetsConf.getApplicationName();
    return new Sheets.Builder(trustedTransport, JSON_FACTORY, credential)
        .setApplicationName(applicationName)
        .build();
  }

  public Spreadsheet getSheet(String idUser, String idSheet) {
    return getSheet(idSheet, sheetsConf.loadCredential(idUser));
  }

  public Spreadsheet getSheet(String idSheet, Credential credential) {
    Sheets sheetsService = initService(sheetsConf, credential);
    try {
      return sheetsService.spreadsheets().get(idSheet)
          .setRanges(List.of("A1:H3"))
          .setIncludeGridData(true)
          .execute();
    } catch (GoogleJsonResponseException e) {
      if (e.getStatusCode() == 401) {
        throw new ForbiddenException(
            "Google Calendar Token is expired or invalid. Give your consent again.");
      }
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return null;
  }
}
