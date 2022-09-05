package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.model.SwanUser;
import app.bpartners.api.repository.swan.response.UserResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class UserSwanRepositoryImpl implements UserSwanRepository {
  private static final String query = "{\"query\":\"query ProfilePage "
      + "{user { id firstName lastName mobilePhoneNumber identificationStatus idVerified "
      + "nationalityCCA3}}\"}";
  private final SwanApi<UserResponse> swanApi;

  @Override
  public SwanUser whoami() {
    try {
      return swanApi.getData(UserResponse.class, query, null)
          .getData()
          .getSwanUser();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}