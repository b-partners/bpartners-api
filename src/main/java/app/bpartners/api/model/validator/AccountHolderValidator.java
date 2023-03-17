package app.bpartners.api.model.validator;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class AccountHolderValidator implements Consumer<AccountHolder> {

  @Override
  public void accept(AccountHolder accountHolder) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (accountHolder.getLocation() != null) {
      if (accountHolder.getLocation().getLongitude() == null) {
        exceptionMessageBuilder.append("Longitude is mandatory. ");
      }
      if (accountHolder.getLocation().getLatitude() == null) {
        exceptionMessageBuilder.append("Latitude is mandatory. ");
      }
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
