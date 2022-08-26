package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.repository.api.swan.SwanApi;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.response.UserResponse;
import app.bpartners.api.repository.swan.schema.SwanUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class UserSwanRepositoryImpl implements UserSwanRepository {
  private static String message = "{\"query\":\"query ProfilePage "
      + "{user { id firstName lastName mobilePhoneNumber identificationStatus idVerified "
      + "birthDate "
      + "nationalityCCA3}}\"}";
  private final SwanApi<UserResponse> swanApi;

  @Override
  public SwanUser whoami() {
    return swanApi.getData(message,"").data.user;
  }
}