package app.bpartners.api.service.subscription;

import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.YearMonth;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionInvoiceTitleComputer implements Function<YearMonth, String> {
  private final CustomDateFormatter customDateFormatter;

  @Override
  public String apply(YearMonth yearMonth) {
    return "Facture " + monthPeriodOf(yearMonth);
  }

  public String monthPeriodOf(YearMonth yearMonth) {
    return "pour la période de "
        + customDateFormatter.formatFrenchDate(yearMonth.atDay(1))
        + " au "
        + customDateFormatter.formatFrenchDate(yearMonth.atEndOfMonth());
  }
}
