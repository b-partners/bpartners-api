package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.User;
import org.springframework.stereotype.Component;

@Component
public class UserRestMapper {
  public User toRest(app.bpartners.api.model.User domain) {
    User user = new User();
    user.setId(domain.getId());
    user.setFirstName(domain.getFirstName());
    user.setLastName(domain.getLastName());
    user.setPhone(domain.getMobilePhoneNumber());
    user.setMonthlySubscriptionAmount(domain.getMonthlySubscription());
    user.setStatus(domain.getStatus());
    return user;
  }
}
