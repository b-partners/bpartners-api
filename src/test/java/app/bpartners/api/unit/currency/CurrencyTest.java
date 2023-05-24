package app.bpartners.api.unit.currency;

import app.bpartners.api.model.Currency;
import app.bpartners.api.service.utils.CurrenyUtils;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CurrencyTest {
  @Test
  void currency_manipulation_ok() {
    String string = "2/3";
    int testInteger = 200;
    MonetaryAmount monetaryAmount = Monetary.getDefaultAmountFactory()
        .setCurrency("EUR")
        .setNumber(200)
        .create();

    Currency fromString = CurrenyUtils.parseCurrency(string);
    Currency fromInteger = CurrenyUtils.parseCurrency(testInteger);
    Currency fromMonetaryAmount = CurrenyUtils.parseCurrency(monetaryAmount);

    assertEquals(1.0, fromString.getApproximatedValue());
    assertEquals(testInteger, fromInteger.getApproximatedValue());
    assertEquals(2, fromInteger.getCentsAsDecimal());
    assertEquals(200, fromInteger.getCentsRoundUp());
    assertEquals(200, fromMonetaryAmount.getApproximatedValue());

  }
}
