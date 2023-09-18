package app.bpartners.api.repository;

import app.bpartners.api.model.AccessToken;

public interface CalendarStoredCredentialRepository {
  AccessToken findLatestByIdUser(String idUser);
}
