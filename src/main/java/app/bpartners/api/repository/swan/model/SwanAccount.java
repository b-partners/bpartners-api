package app.bpartners.api.repository.swan.model;


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;

public class SwanAccount {
  private String id;
  public static final String JSON_PROPERTY_ID = "id";

  private String name;
  public static final String JSON_PROPERTY_NAME = "name";

  private String iban;
  public static final String JSON_PROPERTY_IBAN = "IBAN";

  private String bic;
  public static final String JSON_PROPERTY_BIC = "BIC";


  @Nullable
  @JsonProperty(JSON_PROPERTY_ID)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getId() {
    return id;
  }

  @Nullable
  @JsonProperty(JSON_PROPERTY_NAME)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getName() {
    return name;
  }

  @Nullable
  @JsonProperty(JSON_PROPERTY_IBAN)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getIban() {
    return iban;
  }

  @Nullable
  @JsonProperty(JSON_PROPERTY_BIC)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public String getBic() {
    return bic;
  }
}
