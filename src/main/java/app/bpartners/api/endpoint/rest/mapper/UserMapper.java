package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.SwanUser;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserMapper {
  private final SwanComponent swanComponent;

  public User toRestUser(app.bpartners.api.model.User user) {
    SwanUser swanUser = swanComponent.whoami();
    User restUser = new User();
    restUser.setId(user.getId());
    restUser.setId(user.getSwanUserId());
    restUser.setFirstName(swanUser.getFirstName());
    restUser.setLastName(swanUser.getLastName());
    restUser.setBirthDate(swanUser.getBirthDate());
    restUser.setIdVerified(swanUser.getIdVerified());
    restUser.setIdentificationStatus(swanUser.getIdentificationStatus());
    restUser.setMonthlySubscription(user.getMonthlySubscription());
    restUser.setNationalityCCA3(swanUser.getNationalityCCA3());
    restUser.setMobilePhoneNumber(user.getPhoneNumber());
    restUser.setStatus(EnableStatus.fromValue(user.getStatus().toString()));
    return restUser;
  }
}
