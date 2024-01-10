package app.bpartners.api.repository.google.calendar.drive;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.google.calendar.CalendarConf.JSON_FACTORY;

import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.google.sheets.SheetConf;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.IOException;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
    return getSpreadSheets(sheetConf.googleCredential());
  }

  public FileList getSpreadSheets(Credential credential) {
    String spreadSheetQuery =
        "mimeType='"
            + EXCEL_MIME_TYPE
            + "'"
            + " or mimeType='"
            + GOOGLE_SHEET_MIME_TYPE
            + "'"
            + "and ('me' in owners or sharedWithMe)";
    return getFiles(credential, spreadSheetQuery);
  }

  public File getFileByIdUserAndName(String idUser, String fileName) {
    FileList sheets = getSpreadSheets(idUser);
    var files =
        sheets.getFiles().stream()
            .filter(sheet -> sheet.getName().equals(fileName))
            .collect(Collectors.toList());
    if (files.isEmpty()) {
      throw new NotFoundException(
          "File(name=" + fileName + ") does not exist or you do not have authorization to read it");
    } else if (files.size() > 1) {
      throw new NotImplementedException(
          "There are " + files.size() + " files with name = " + fileName);
    }
    return files.get(0);
  }

  private FileList getFiles(Credential credential, String query) {
    Drive driveService = initService(sheetConf, credential);
    try {
      return driveService.files().list().setQ(query).setFields(defaultFields()).execute();
    } catch (GoogleJsonResponseException e) {
      switch (e.getStatusCode()) {
        case 400:
          throw new BadRequestException("[Google Drive] " + e.getDetails().getMessage());
        case 403:
          throw new ForbiddenException("[Google Drive] " + e.getDetails().getMessage());
        case 401:
          throw new ForbiddenException(
              "Google Drive/Sheet Token is expired or invalid. Give your consent again.");
        default:
          throw new ApiException(SERVER_EXCEPTION, e);
      }
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private static String defaultFields() {
    return "files(id, name,mimeType,permissions)";
  }
}
