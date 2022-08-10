package app.bpartners.api.model.validator;

import java.util.regex.Pattern;


public class EmailValidator {
  public boolean accept(String email) {
    String regexPattern = "^(?=.{1,64}@)[A-Za-z0-9_-]" + "+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$";
    return Pattern.compile(regexPattern).matcher(email).matches();
  }

}
