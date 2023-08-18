package app.bpartners.api.repository.google.calendar.drive;

import app.bpartners.api.repository.google.sheets.SheetConf;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.repository.google.calendar.CalendarConf.JSON_FACTORY;

@Slf4j
@Data
@Component
@AllArgsConstructor
public class DriveApi {
  private final SheetConf sheetConf;

  private Drive initService(SheetConf sheetsConf, Credential credential) {
    NetHttpTransport trustedTransport = sheetsConf.getTrustedTransport();
    String applicationName = sheetsConf.getApplicationName();
    return new Drive.Builder(trustedTransport, JSON_FACTORY, credential)
        .setApplicationName(applicationName)
        .build();
  }

  public FileList getSpreadSheets(String idUser) {
    return getSpreadSheets(sheetConf.getLocalCredentials(idUser));
  }

  @SneakyThrows
  public FileList getSpreadSheets(Credential credential) {
    Drive driveService = initService(sheetConf, credential);
    return driveService.files().list()
        .setQ(
            "mimeType='application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'"
                + " or mimeType='application/vnd.google-apps.spreadsheet'"
                + "and 'me' in owners and sharedWithMe")
        .setFields("files(id, name,mimeType)")
        .execute();
  }
}
