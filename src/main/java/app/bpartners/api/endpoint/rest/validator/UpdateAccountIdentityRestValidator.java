package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.UpdateAccountIdentity;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UpdateAccountIdentityRestValidator implements Consumer<UpdateAccountIdentity> {
  private final BICValidator bicValidator;
  private final IBANValidator ibanValidator;

  @Override
  public void accept(UpdateAccountIdentity accountIdentity) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (accountIdentity.getBic() == null) {
      exceptionMessageBuilder.append("bic is mandatory. ");
    } else {
      bicValidator.accept(accountIdentity.getBic());
    }
    if (accountIdentity.getIban() == null) {
      exceptionMessageBuilder.append("iban is mandatory. ");
    } else {
      ibanValidator.accept(accountIdentity.getIban());
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
