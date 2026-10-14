package app.bpartners.api.service.invoice;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.User;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.thymeleaf.context.Context;

class InvoiceTemplateCreditNoteTest {
  @Test
  void renders_credit_note_wording_for_a_credit_note() {
    var html = render(invoice(true, false));

    assertTrue(html.contains("FACTURE AVOIR"));
    assertTrue(html.contains("Facture avoir n° :"));
    assertFalse(html.contains("Facture n° :"));
  }

  @Test
  void renders_credit_note_wording_for_a_subscription_credit_note() {
    var html = render(invoice(true, true));

    assertTrue(html.contains("FACTURE AVOIR"));
    assertTrue(html.contains("Facture avoir n° :"));
    assertTrue(html.contains("subscription-invoice"));
  }

  @Test
  void renders_regular_wording_for_a_regular_invoice() {
    var html = render(invoice(false, false));

    assertTrue(html.contains("FACTURE"));
    assertTrue(html.contains("Facture n° :"));
    assertFalse(html.contains("FACTURE AVOIR"));
    assertFalse(html.contains("Facture avoir n° :"));
  }

  @Test
  void renders_regular_wording_for_a_regular_subscription_invoice() {
    var html = render(invoice(false, true));

    assertTrue(html.contains("Facture n° :"));
    assertFalse(html.contains("FACTURE AVOIR"));
    assertFalse(html.contains("Facture avoir n° :"));
  }

  @Test
  void keeps_the_reference_after_the_credit_note_label() {
    var html = render(invoice(true, false));

    assertTrue(html.contains("REF-2026-1"));
  }

  private static String render(Invoice invoice) {
    var context = new Context();
    context.setVariable("invoice", invoice);
    context.setVariable("logo", null);
    context.setVariable("account", invoice.getActualAccount());
    context.setVariable("accountHolder", invoice.getActualHolder());
    return new TemplateResolverEngine().getTemplateEngine().process("invoice", context);
  }

  private static Invoice invoice(boolean creditNote, boolean subscriptionInvoice) {
    var account =
        Account.builder()
            .id("account_id")
            .name("BPartners SAS")
            .iban("FR7630006000011234567890189")
            .bic("AGRIFRPP")
            .active(true)
            .build();
    var accountHolder =
        AccountHolder.builder()
            .name("BPartners SAS")
            .address("229 rue Saint-Honoré")
            .postalCode("75001")
            .city("Paris")
            .country("France")
            .socialCapital(0)
            .siren("123456789")
            .vatNumber("FR12345678901")
            .subjectToVat(true)
            .build();
    var user =
        User.builder()
            .id("user_id")
            .accounts(List.of(account))
            .accountHolders(List.of(accountHolder))
            .build();
    return Invoice.builder()
        .id("invoice_id")
        .ref("REF-2026-1")
        .user(user)
        .creditNote(creditNote)
        .subscriptionInvoice(subscriptionInvoice)
        .products(List.of())
        .discount(
            InvoiceDiscount.builder()
                .percentValue(new Fraction())
                .amountValue(new Fraction())
                .build())
        .totalPriceWithoutDiscount(new Fraction())
        .totalPriceWithoutVat(new Fraction())
        .totalVat(new Fraction())
        .totalPriceWithVat(new Fraction())
        .delayPenaltyPercent(new Fraction())
        .build();
  }
}
