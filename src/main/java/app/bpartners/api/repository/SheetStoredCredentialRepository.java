package app.bpartners.api.repository;

import app.bpartners.api.model.AccessToken;

public interface SheetStoredCredentialRepository {
  AccessToken findLatestByIdUser(String idUser);
}
