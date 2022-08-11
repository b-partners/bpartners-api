package app.bpartners.api.graphql;

import app.bpartners.api.endpoint.rest.model.SwanUser;
import org.springframework.stereotype.Component;

@Component
public class SwanMapper {
  public SwanUser graphQLToRest(app.bpartners.api.graphql.schemas.SwanUser graphqlUser) {
    SwanUser user = new SwanUser();
    user.setId(graphqlUser.id);
    user.setFirstName(graphqlUser.firstName);
    user.setLastName(graphqlUser.lastName);
    user.setMobilePhoneNumber(graphqlUser.mobilePhoneNumber);
    user.setIdentificationStatus(graphqlUser.identificationStatus);
    user.setBirthDate(graphqlUser.birthDate);
    user.setNationalityCCA3(graphqlUser.nationalityCCA3);
    user.setIdVerified(graphqlUser.idVerified);
    return user;
  }
}
