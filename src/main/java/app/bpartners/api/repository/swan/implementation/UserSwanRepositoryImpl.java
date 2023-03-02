package app.bpartners.api.repository.swan.implementation;

import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.UserSwanRepository;
import app.bpartners.api.repository.swan.model.SwanUser;
import app.bpartners.api.repository.swan.response.UserResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class UserSwanRepositoryImpl implements UserSwanRepository {
  public static final String QUERY = "{\"query\":\"query ProfilePage "
      + "{user { id firstName lastName mobilePhoneNumber identificationStatus idVerified "
      + "idVerified identificationStatus}}\"}";

  private final SwanApi<UserResponse> swanApi;
  private final SwanCustomApi<UserResponse> swanCustomApi;

  @Override
  public SwanUser whoami() {
    UserResponse data = swanApi.getData(UserResponse.class, QUERY);
    return data == null ? null : data.getData().getUser();
  }

  @Override
  public SwanUser getByToken(String token) {
    UserResponse data = swanCustomApi.getData(UserResponse.class, QUERY, token);
    return data == null ? null : data.getData().getUser();
  }
}