package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CustomEmail;
import app.bpartners.api.endpoint.rest.validator.CustomEmailValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CustomEmailRestMapper {
  private final CustomEmailValidator validator;

  public CustomEmail toRest(app.bpartners.api.model.CustomEmail domain) {
    return new CustomEmail()
        .subject(domain.getSubject())
        .message(domain.getBody());
  }

  public app.bpartners.api.model.CustomEmail toDomain(CustomEmail rest) {
    validator.accept(rest);
    return app.bpartners.api.model.CustomEmail.builder()
        .subject(rest.getSubject())
        .body(rest.getMessage())
        .build();
  }
}
