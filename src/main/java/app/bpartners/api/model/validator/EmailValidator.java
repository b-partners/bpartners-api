package app.bpartners.api.model.validator;

import app.bpartners.api.model.Email;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class EmailValidator {
  private final String regex = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\\\.[A-Za-z0-9_-]+)" +
      "*@[^-][A-Za-z0-9-]+(\\\\.[A-Za-z0-9-]+)*(\\\\.[A-Za-z]{2,})$";

  public boolean patternMatches(Email mail) {
    return Pattern.compile(regex)
        .matcher(mail.getEmail())
        .matches();
  }

  public void accept(Email email){
    if(!patternMatches(email)){
      throw new BadRequestException("{"
          + "\"type\":\"400 BAD REQUEST\","
          + "\"message\":\"Invalid email !\"}");
    }
  }

}
