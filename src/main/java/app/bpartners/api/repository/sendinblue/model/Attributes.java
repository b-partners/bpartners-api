package app.bpartners.api.repository.sendinblue.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Setter;
import lombok.ToString;

@Setter
@Builder
@ToString
public class Attributes {
  private Double id;
  public static final String JSON_PROPERTY_ID = "ID";
  private String lastName;
  public static final String JSON_PROPERTY_FIRSTNAME = "PRENOM";
  private String firstName;
  public static final String JSON_PROPERTY_LASTNAME = "NOM";
  private Double phone;
  public static final String JSON_PROPERTY_PHONE = "TELEPHONE";
  private String smsPhoneNumber;
  public static final String JSON_PROPERTY_SMS = "SMS";

  @JsonProperty(JSON_PROPERTY_ID)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Double getId() {
    return id;
  }

  @JsonProperty(JSON_PROPERTY_LASTNAME)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getLastName() {
    return lastName;
  }

  @JsonProperty(JSON_PROPERTY_FIRSTNAME)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getFirstName() {
    return firstName;
  }

  @JsonProperty(JSON_PROPERTY_PHONE)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Double getPhone() {
    return phone;
  }

  @JsonProperty(JSON_PROPERTY_SMS)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getSmsPhoneNumber() {
    return smsPhoneNumber;
  }
}
