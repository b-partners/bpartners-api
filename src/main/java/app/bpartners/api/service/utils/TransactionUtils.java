package app.bpartners.api.service.utils;

import app.bpartners.api.repository.jpa.model.HTransaction;
import java.util.List;

public class TransactionUtils {
  private TransactionUtils() {}

  public static String describeList(List<HTransaction> list) {
    StringBuilder builder = new StringBuilder();
    list.forEach(
        transaction -> {
          builder.append(transaction.describe()).append(" ");
        });
    String message = builder.toString();
    if (!message.isEmpty()) {
      return message;
    }
    return null;
  }
}
