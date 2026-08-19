package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateCreditPackPurchase;
import app.bpartners.api.endpoint.rest.model.CreateCreditPurchase;
import app.bpartners.api.endpoint.rest.model.CreateCustomCreditPurchase;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateCreditPurchaseRestValidator implements Consumer<CreateCreditPurchase> {
  public static final long MAX_CUSTOM_CREDITS_PER_PURCHASE = 10_000L;

  @Override
  public void accept(CreateCreditPurchase createCreditPurchase) {
    var messageBuilder = new StringBuilder();
    if (createCreditPurchase.getType() == null) {
      messageBuilder.append("CreateCreditPurchase.type is mandatory. ");
    }
    appendRedirectionViolations(createCreditPurchase, messageBuilder);
    if (createCreditPurchase instanceof CreateCreditPackPurchase packPurchase) {
      appendPackViolations(packPurchase, messageBuilder);
    }
    if (createCreditPurchase instanceof CreateCustomCreditPurchase customPurchase) {
      appendCustomViolations(customPurchase, messageBuilder);
    }
    var errorMessage = messageBuilder.toString().trim();
    if (!errorMessage.isEmpty()) {
      throw new BadRequestException(errorMessage);
    }
  }

  private void appendRedirectionViolations(
      CreateCreditPurchase createCreditPurchase, StringBuilder messageBuilder) {
    var redirectionStatusUrls = createCreditPurchase.getRedirectionStatusUrls();
    if (redirectionStatusUrls == null) {
      messageBuilder.append("CreateCreditPurchase.redirectionStatusUrls is mandatory. ");
      return;
    }
    if (redirectionStatusUrls.getSuccessUrl() == null) {
      messageBuilder.append("CreateCreditPurchase.redirectionStatusUrls.successUrl is mandatory. ");
    }
    if (redirectionStatusUrls.getFailureUrl() == null) {
      messageBuilder.append("CreateCreditPurchase.redirectionStatusUrls.failureUrl is mandatory. ");
    }
  }

  private void appendPackViolations(
      CreateCreditPackPurchase packPurchase, StringBuilder messageBuilder) {
    if (packPurchase.getCreditPackIdentifier() == null) {
      messageBuilder.append("CreateCreditPackPurchase.creditPackIdentifier is mandatory. ");
    }
    if (packPurchase.getQuantity() != null && packPurchase.getQuantity() < 1) {
      messageBuilder.append("CreateCreditPackPurchase.quantity must be at least 1. ");
    }
  }

  private void appendCustomViolations(
      CreateCustomCreditPurchase customPurchase, StringBuilder messageBuilder) {
    var credits = customPurchase.getCredits();
    if (credits == null || credits < 1) {
      messageBuilder.append("CreateCustomCreditPurchase.credits must be at least 1. ");
    } else if (credits > MAX_CUSTOM_CREDITS_PER_PURCHASE) {
      messageBuilder.append(
          "CreateCustomCreditPurchase.credits must be at most "
              + MAX_CUSTOM_CREDITS_PER_PURCHASE
              + ". ");
    }
  }
}
