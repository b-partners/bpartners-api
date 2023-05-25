package app.bpartners.api.endpoint.rest.security.bridge;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class BridgeConf {
  public static final String FRANCE_BANK_COUNTRY_CODE = "fr";
  private String clientId;
  private String clientSecret;
  private String baseUrl;
  private String bridgeVersion;

  public BridgeConf(
      @Value("${bridge.client.id}")
      String clientId,
      @Value("${bridge.client.secret}")
      String clientSecret,
      @Value("${bridge.base.url}")
      String baseUrl,
      @Value("${bridge.version}")
      String bridgeVersion
  ) {
    this.clientId = clientId;
    this.clientSecret = clientSecret;
    this.baseUrl = baseUrl;
    this.bridgeVersion = bridgeVersion;
  }

  public String getUserUrl() {
    return baseUrl + "/users";
  }

  public String getAuthUrl() {
    return baseUrl + "/authenticate";
  }

  public String getAddItemUrl() {
    return baseUrl + "/connect/items/add";
  }

  public String getGetItemUrl() {
    return baseUrl + "/items";
  }

  public String getItemByIdUrl(String id) {
    return baseUrl + "/items/" + id;
  }

  public String getRefreshUrl(Long itemId) {
    return baseUrl + "/items/" + itemId + "/refresh";
  }

  public String getTransactionUpdatedUrl() {
    return baseUrl + "/transactions/updated";
  }

  public String getTransactionUrl() {
    return baseUrl + "/transactions";
  }

  public String getPaginatedTransactionUrl(String uri) {
    return baseUrl + uri.replace("/v2", "");
  }

  public String getItemStatusUrl(Long id) {
    return baseUrl + "/items/" + id + "/refresh/status";
  }

  public String getProItemsValidationUrl() {
    return baseUrl + "/connect/items/pro/confirmation";
  }

  public String getEditItemsUrl() {
    return baseUrl + "/connect/items/edit?item_id=";
  }

  public String getScaSyncUrl() {
    return baseUrl + "/connect/items/sync";
  }

  public String getAccountUrl() {
    return baseUrl + "/accounts";
  }

  public String getBankUrl() {
    return baseUrl + "/banks";
  }

}
