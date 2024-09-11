package app.bpartners.api.endpoint.rest.validator;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.CLIENT_EXCEPTION;

import app.bpartners.api.model.exception.ApiException;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class BICValidator implements Consumer<String> {
  private static final String BIC_REGEX = "^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}([A-Z0-9]{3})?$";

  @Override
  public void accept(String bic) {
    if (!isValidLength(bic) || !isValidFormat(bic)) {
      throw new ApiException(CLIENT_EXCEPTION, "Provided BIC=" + bic + " is not valid");
    }
  }

  private boolean isValidLength(String bic) {
    return bic != null && (bic.length() == 8 || bic.length() == 11);
  }

  private boolean isValidFormat(String bic) {
    return Pattern.compile(BIC_REGEX).matcher(bic).matches();
  }
}
