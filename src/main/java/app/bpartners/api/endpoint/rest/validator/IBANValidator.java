package app.bpartners.api.endpoint.rest.validator;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.CLIENT_EXCEPTION;

import app.bpartners.api.model.exception.ApiException;
import java.math.BigInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class IBANValidator implements Consumer<String> {
  private static final String IBAN_REGEX = "^[A-Z]{2}\\d{2}[A-Z0-9]{1,30}$";

  @Override
  public void accept(String iban) {
    if (!isValidLength(iban) || !isValidFormat(iban) || !isValidChecksum(iban)) {
      throw new ApiException(CLIENT_EXCEPTION, "Provided IBAN=" + iban + " is not valid");
    }
  }

  // IBAN length must be between 15 and 34 chars, country dependant
  private boolean isValidLength(String iban) {
    return iban != null && iban.length() >= 15 && iban.length() <= 34;
  }

  private boolean isValidFormat(String iban) {
    return Pattern.compile(IBAN_REGEX).matcher(iban).matches();
  }

  // Verify IBAN checksum
  private boolean isValidChecksum(String iban) {
    var rearrangedIban = iban.substring(4) + iban.substring(0, 4);

    var numericIban = new StringBuilder();
    for (char ch : rearrangedIban.toCharArray()) {
      if (Character.isLetter(ch)) {
        numericIban.append(Character.getNumericValue(ch));
      } else {
        numericIban.append(ch);
      }
    }
    var ibanAsNumber = new BigInteger(numericIban.toString());
    // IBAN is valid only if mod97 == 1
    return ibanAsNumber.mod(BigInteger.valueOf(97)).intValue() == 1;
  }
}
