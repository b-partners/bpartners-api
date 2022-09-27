package app.bpartners.api.repository.sendinblue;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import sendinblue.ApiClient;
import sendinblue.auth.ApiKeyAuth;

@Configuration
@Getter
public class SendinblueConf {
  public static final String API_KEY = "api-key";
  private final String apiKey;
  private final Long contactListId;
  private final ApiClient defaultClient;

  public SendinblueConf(
      @Value("${sendinblue.api.key}")
      String apiKey,
      @Value("${sendinblue.list.id}")
      Long contactListId) {
    this.apiKey = apiKey;
    this.contactListId = contactListId;
    this.defaultClient = sendinblue.Configuration.getDefaultApiClient();
    ApiKeyAuth auth = (ApiKeyAuth) defaultClient.getAuthentication(API_KEY);
    auth.setApiKey(apiKey);
  }
}
