package app.bpartners.api.service.accountholder;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class AccountHolderInvoiceValidator implements Consumer<AccountHolder> {
  @Override
  public void accept(AccountHolder accountHolder) {
    StringBuilder builder = new StringBuilder();
    if (accountHolder.getAddress() == null) {
      builder.append("Account holder address is mandatory to confirm invoice");
    }
    if (accountHolder.getCountry() == null) {
      builder.append("Account holder country is mandatory to confirm invoice");
    }
    if (accountHolder.getCity() == null) {
      builder.append("Account holder city is mandatory to confirm invoice");
    }
    if (accountHolder.getPostalCode() == null) {
      builder.append("Account holder postal code is mandatory to confirm invoice");
    }
    String message = builder.toString();
    if (!message.isEmpty()) {
      throw new BadRequestException(message);
    }
  }
}
