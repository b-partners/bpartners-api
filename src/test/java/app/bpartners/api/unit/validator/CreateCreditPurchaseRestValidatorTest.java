package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.endpoint.rest.model.CreateCreditPackPurchase;
import app.bpartners.api.endpoint.rest.model.CreateCreditPurchase;
import app.bpartners.api.endpoint.rest.model.CreateCustomCreditPurchase;
import app.bpartners.api.endpoint.rest.model.CreditPurchaseType;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.CreateCreditPurchaseRestValidator;
import app.bpartners.api.model.exception.BadRequestException;
import org.junit.jupiter.api.Test;

class CreateCreditPurchaseRestValidatorTest {
  CreateCreditPurchaseRestValidator subject = new CreateCreditPurchaseRestValidator();

  private RedirectionStatusUrls bothUrls() {
    return new RedirectionStatusUrls()
        .successUrl("https://birdia.fr/success")
        .failureUrl("https://birdia.fr/failure");
  }

  @Test
  void accept_a_valid_pack_purchase() {
    assertDoesNotThrow(
        () ->
            subject.accept(
                new CreateCreditPackPurchase()
                    .creditPackIdentifier("pack_10")
                    .quantity(2)
                    .type(CreditPurchaseType.PACK)
                    .redirectionStatusUrls(bothUrls())));
  }

  @Test
  void accept_a_valid_custom_purchase() {
    assertDoesNotThrow(
        () ->
            subject.accept(
                new CreateCustomCreditPurchase()
                    .credits(7L)
                    .type(CreditPurchaseType.CUSTOM)
                    .redirectionStatusUrls(bothUrls())));
  }

  @Test
  void reject_a_missing_type() {
    var exception =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new CreateCustomCreditPurchase()
                        .credits(7L)
                        .redirectionStatusUrls(bothUrls())));

    assertEquals("CreateCreditPurchase.type is mandatory.", exception.getMessage());
  }

  @Test
  void reject_missing_redirection_status_urls() {
    var exception =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new CreateCustomCreditPurchase().credits(7L).type(CreditPurchaseType.CUSTOM)));

    assertEquals(
        "CreateCreditPurchase.redirectionStatusUrls is mandatory.", exception.getMessage());
  }

  @Test
  void reject_partial_redirection_status_urls() {
    var exception =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new CreateCustomCreditPurchase()
                        .credits(7L)
                        .type(CreditPurchaseType.CUSTOM)
                        .redirectionStatusUrls(new RedirectionStatusUrls())));

    assertEquals(
        "CreateCreditPurchase.redirectionStatusUrls.successUrl is mandatory."
            + " CreateCreditPurchase.redirectionStatusUrls.failureUrl is mandatory.",
        exception.getMessage());
  }

  @Test
  void reject_a_pack_purchase_without_pack_identifier() {
    var exception =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new CreateCreditPackPurchase()
                        .type(CreditPurchaseType.PACK)
                        .redirectionStatusUrls(bothUrls())));

    assertEquals(
        "CreateCreditPackPurchase.creditPackIdentifier is mandatory.", exception.getMessage());
  }

  @Test
  void reject_a_non_positive_quantity() {
    var exception =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new CreateCreditPackPurchase()
                        .creditPackIdentifier("pack_10")
                        .quantity(0)
                        .type(CreditPurchaseType.PACK)
                        .redirectionStatusUrls(bothUrls())));

    assertEquals("CreateCreditPackPurchase.quantity must be at least 1.", exception.getMessage());
  }

  @Test
  void reject_a_non_positive_credits_amount() {
    var exception =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new CreateCustomCreditPurchase()
                        .credits(0L)
                        .type(CreditPurchaseType.CUSTOM)
                        .redirectionStatusUrls(bothUrls())));

    assertEquals("CreateCustomCreditPurchase.credits must be at least 1.", exception.getMessage());
  }

  @Test
  void reject_a_missing_credits_amount() {
    assertThrows(
        BadRequestException.class,
        () ->
            subject.accept(
                new CreateCustomCreditPurchase()
                    .type(CreditPurchaseType.CUSTOM)
                    .redirectionStatusUrls(bothUrls())));
  }

  @Test
  void reject_a_credits_amount_over_the_cap() {
    var exception =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new CreateCustomCreditPurchase()
                        .credits(
                            CreateCreditPurchaseRestValidator.MAX_CUSTOM_CREDITS_PER_PURCHASE + 1)
                        .type(CreditPurchaseType.CUSTOM)
                        .redirectionStatusUrls(bothUrls())));

    assertEquals(
        "CreateCustomCreditPurchase.credits must be at most 10000.", exception.getMessage());
  }

  @Test
  void reject_a_bare_create_credit_purchase() {
    assertThrows(
        BadRequestException.class,
        () -> subject.accept(new CreateCreditPurchase().type(CreditPurchaseType.PACK)));
  }

  @Test
  void accept_a_pack_purchase_without_explicit_quantity() {
    assertDoesNotThrow(
        () ->
            subject.accept(
                new CreateCreditPackPurchase()
                    .creditPackIdentifier("pack_10")
                    .quantity(null)
                    .type(CreditPurchaseType.PACK)
                    .redirectionStatusUrls(bothUrls())));
  }
}
