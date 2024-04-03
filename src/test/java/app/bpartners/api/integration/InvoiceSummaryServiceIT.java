package app.bpartners.api.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.InvoiceSummary;
import app.bpartners.api.model.Money;
import app.bpartners.api.model.User;
import app.bpartners.api.service.InvoiceSummaryService;
import app.bpartners.api.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
public class InvoiceSummaryServiceIT extends MockedThirdParties {
  @Autowired InvoiceSummaryService subject;
  @Autowired UserService userService;

  @Test
  void update_invoices_summaries_ok() {
    List<String> enabledUsersIds =
        userService.findAll().stream()
            .filter(user -> user.getStatus() == EnableStatus.ENABLED)
            .map(User::getId)
            .toList();

    List<InvoiceSummary> actual =
        enabledUsersIds.stream().map(userId -> subject.updateInvoiceSummary(userId)).toList();

    assertEquals(enabledUsersIds.size(), actual.size());
    assertTrue(
        ignoreUpdateDatetime(actual)
            .contains(
                InvoiceSummary.builder()
                    .updatedAt(null)
                    .paid(
                        InvoiceSummary.InvoiceSummaryContent.builder()
                            .amount(Money.fromMajor(4400))
                            .build())
                    .unpaid(
                        InvoiceSummary.InvoiceSummaryContent.builder()
                            .amount(Money.fromMajor(5500))
                            .build())
                    .proposal(
                        InvoiceSummary.InvoiceSummaryContent.builder()
                            .amount(Money.fromMajor(6700))
                            .build())
                    .build()));
  }

  List<InvoiceSummary> ignoreUpdateDatetime(List<InvoiceSummary> summaries) {
    return summaries.stream().peek(summary -> summary.setUpdatedAt(null)).toList();
  }
}
