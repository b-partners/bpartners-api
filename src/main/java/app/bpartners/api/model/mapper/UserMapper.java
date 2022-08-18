package app.bpartners.api.model.mapper;

import app.bpartners.api.model.User;
import app.bpartners.api.model.entity.HUser;
import app.bpartners.api.repository.swan.schema.SwanUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserMapper {
  private final SwanUserMapper swanUserMapper;

  public User toDomain(HUser hUser, SwanUser swanUser) {
    return User.builder()
        .id(hUser.getId())
        .swanUser(swanUserMapper.toDomain(swanUser))
        .monthlySubscription(hUser.getMonthlySubscription())
        .status(hUser.getStatus())
        .build();
  }

  public HUser toHibernate(User user) {
    return HUser.builder()
        .id(user.getId())
        .monthlySubscription(user.getMonthlySubscription())
        .swanUserId(user.getSwanUser().getId())
        .status(user.getStatus())
        .phoneNumber(user.getSwanUser().getMobilePhoneNumber())
        .build();
  }

  public app.bpartners.api.endpoint.rest.model.User toRest(User user){
    app.bpartners.api.endpoint.rest.model.User restUser = new app.bpartners.api.endpoint.rest.model.User();
    restUser.setId(user.getId());
    restUser.setBirthDate(user.getSwanUser().getBirthDate());
    restUser.setFirstName(user.getSwanUser().getFirstName());
    restUser.setLastName(user.getSwanUser().getLastName());
    restUser.setStatus(user.getStatus());
    restUser.setIdentificationStatus(user.getSwanUser().getIdentificationStatus());
    restUser.setMonthlySubscription(user.getMonthlySubscription());
    restUser.setIdVerified(user.getSwanUser().getIdVerified());
    restUser.setNationalityCCA3(user.getSwanUser().getNationalityCCA3());
    restUser.setSwanId(user.getSwanUser().getId());
    restUser.setMobilePhoneNumber(user.getSwanUser().getMobilePhoneNumber());
    return restUser;
  }
}
