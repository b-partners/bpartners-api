package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.model.SwanUser;
import org.springframework.stereotype.Component;

@Component
public class UserRestMapper {
  public User toRest(app.bpartners.api.model.User domain) {
    SwanUser domainSwanUser = domain.getSwanUser();
    User user = new User();
    user.setId(domain.getId());
    user.setFirstName(domainSwanUser.getFirstName());
    user.setLastName(domainSwanUser.getLastName());
    user.setBirthDate(domainSwanUser.getBirthDate());
    user.setPhone(domainSwanUser.getMobilePhoneNumber());
    user.setIdVerified(domainSwanUser.getIdVerified());
    user.setNationalityCCA3(domainSwanUser.getNationalityCCA3());
    user.setIdentificationStatus(domainSwanUser.getIdentificationStatus());
    user.setMonthlySubscriptionAmount(domain.getMonthlySubscription());
    user.setStatus(domain.getStatus());
    return user;
  }
}
