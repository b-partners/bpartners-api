package app.bpartners.api.repository.google.sheets;

import app.bpartners.api.repository.google.generic.DbCredentialStore;
import app.bpartners.api.repository.google.sheets.mapper.SheetCredentialMapper;
import app.bpartners.api.repository.jpa.SheetStoredCredentialJpaRep;
import app.bpartners.api.repository.jpa.model.HSheetStoredCredential;
import org.springframework.stereotype.Component;

@Component
public class SheetsDbCredentialStore extends
    DbCredentialStore<HSheetStoredCredential, SheetStoredCredentialJpaRep, SheetCredentialMapper> {
  public SheetsDbCredentialStore(SheetStoredCredentialJpaRep repository,
                                 SheetCredentialMapper mapper) {
    super(repository, mapper);
  }
}

