package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.exception.ApiException;
import org.junit.jupiter.api.Test;

class StripePortalServiceTest {
  StripePortalService subject = new StripePortalService();

  @Test
  void throw_exception_on_null_provided_stripe_customer_for_initiating_billing_portal() {
    var actualException =
        assertThrows(
            ApiException.class,
            () -> subject.initiateBillingPortalSession(null, new RedirectionStatusUrls()));

    assertEquals(
        "Unable to initiate billing portal session as user not associated to stripe customer",
        actualException.getMessage());
    assertEquals(SERVER_EXCEPTION, actualException.getType());
  }
}
