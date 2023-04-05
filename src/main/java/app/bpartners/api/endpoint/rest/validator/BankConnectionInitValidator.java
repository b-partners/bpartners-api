package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class BankConnectionInitValidator implements Consumer<RedirectionStatusUrls> {
  @Override
  public void accept(RedirectionStatusUrls redirectionStatusUrls) {
    RedirectionValidator.verifyRedirectionStatusUrls(new StringBuilder(), redirectionStatusUrls);
  }
}
