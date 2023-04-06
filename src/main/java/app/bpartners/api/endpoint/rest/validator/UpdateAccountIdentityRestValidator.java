package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.UpdateAccountIdentity;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class UpdateAccountIdentityRestValidator implements Consumer<UpdateAccountIdentity> {
  @Override
  public void accept(UpdateAccountIdentity accountIdentity) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (accountIdentity.getBic() == null) {
      exceptionMessageBuilder.append("bic is mandatory.");
    }
    if (accountIdentity.getBic() != null && !isValidBic(accountIdentity.getBic())) {
      exceptionMessageBuilder.append("bic is not valid");
    }
    if (accountIdentity.getIban() != null && !isValidIban(accountIdentity.getIban())) {
      exceptionMessageBuilder.append("iban is not valid");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  private boolean isValidBic(String bic) {
    //TODO: use regExp to match bic
    return true;
  }

  private boolean isValidIban(String iban) {
    //TODO: use regExp to match iban
    return true;
  }
}
