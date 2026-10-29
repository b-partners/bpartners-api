package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.SecondaryUser;
import app.bpartners.api.endpoint.rest.model.SecondaryUserStatus;
import org.springframework.stereotype.Component;

@Component
public class SecondaryUserRestMapper {

  public SecondaryUser toRest(app.bpartners.api.model.subscription.SecondaryUser domain) {
    return new SecondaryUser()
        .id(domain.getId())
        .primaryUserId(domain.getPrimaryUserId())
        .secondaryUserId(domain.getSecondaryUserId())
        .email(domain.getInvitedEmail())
        .status(SecondaryUserStatus.valueOf(domain.getStatus().name()))
        .creationDatetime(domain.getCreationDatetime());
  }
}
