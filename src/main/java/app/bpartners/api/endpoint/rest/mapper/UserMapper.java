package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.SwanUser;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.endpoint.rest.security.swan.SwanComponent;

public class UserMapper {
  public static User toRestUser(app.bpartners.api.model.User user){
    SwanUser swanUser = SwanComponent.getUserById(user.getSwanUserId());
    User restUser = new User();
    restUser.setId(user.getId());
    restUser.setSwanId(user.getSwanUserId());
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
