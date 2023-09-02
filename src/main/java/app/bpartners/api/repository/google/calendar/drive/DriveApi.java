package app.bpartners.api.repository.google.calendar.drive;

import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.google.sheets.SheetConf;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
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
  public static final String EXCEL_MIME_TYPE =
      "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
  public static final String GOOGLE_SHEET_MIME_TYPE = "application/vnd.google-apps.spreadsheet";
  private final SheetConf sheetConf;

  private Drive initService(SheetConf sheetsConf, Credential credential) {
    NetHttpTransport trustedTransport = sheetsConf.getTrustedTransport();
    String applicationName = sheetsConf.getApplicationName();
    return new Drive.Builder(trustedTransport, JSON_FACTORY, credential)
        .setApplicationName(applicationName)
        .build();
  }

  public FileList getSpreadSheets(String idUser) {
    return getSpreadSheets(sheetConf.loadCredential(idUser));
  }

  public FileList getSpreadSheets(Credential credential) {
    String spreadSheetQuery =
        "mimeType='" + EXCEL_MIME_TYPE + "'"
            + " or mimeType='" + GOOGLE_SHEET_MIME_TYPE + "'"
            + "and ('me' in owners or sharedWithMe)";
    return getFiles(credential, spreadSheetQuery);
  }

  public File getFileByIdUserAndName(String idUser, String fileName) {
    FileList sheets = getSpreadSheets(idUser);
    return sheets.getFiles().stream()
        .filter(sheet -> sheet.getName().equals(fileName))
        .findAny().orElseThrow(
            () -> new NotFoundException(
                "File(name=" + fileName
                    + ") does not exist or you do not have authorization to read it"));
  }

  @SneakyThrows
  private FileList getFiles(Credential credential, String query) {
    Drive driveService = initService(sheetConf, credential);
    return driveService.files().list()
        .setQ(query)
        .setFields(defaultFields())
        .execute();
  }

  private static String defaultFields() {
    return "files(id, name,mimeType,permissions)";
  }
}
