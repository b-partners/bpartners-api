package app.bpartners.api.repository.prospecting.datasource.buildingpermit;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

public interface BuildingPermitApiInterface {
  @Retryable(exceptionExpression = "#{exception.message.contains('<!doctype html>')}",
      maxAttempts = 3, backoff = @Backoff(random = true, multiplier = 2, delay = 1_000))
  public <T> T getData(String url, Class<T> valueType);
}
