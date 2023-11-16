package app.bpartners.api.repository.google.sheets;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.google.generic.CredentialConfig;
import java.util.List;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import static com.google.api.services.sheets.v4.SheetsScopes.DRIVE_READONLY;
import static com.google.api.services.sheets.v4.SheetsScopes.SPREADSHEETS;

@Configuration
@Getter
public class SheetConf extends CredentialConfig {
  public static final String GRID_SHEET_TYPE = "GRID";

  public SheetConf(@Value("${google.sheet.apps.name}") String applicationName,
                   @Value("${google.sheet.client.id}") String clientId,
                   @Value("${google.sheet.client.secret}") String clientSecret,
                   @Value("${google.sheet.redirect.uris}") List<String> redirectUris,
                   SheetsDbCredentialStore dbCredentialStore,
                   ProjectTokenManager tokenManager) {
    super(applicationName,
        clientId,
        clientSecret,
        redirectUris,
        allowedScopes(),
        dbCredentialStore,
        tokenManager);
  }

  public static List<String> allowedScopes() {
    return List.of(DRIVE_READONLY, SPREADSHEETS);
  }
}
