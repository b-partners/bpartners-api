package app.bpartners.api.repository.sendinblue.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Builder
public class Contact {
  private String email;
  private static final String JSON_PROPERTY_EMAIL = "email";
  private boolean emailBlackListed;
  private static final String JSON_PROPERTY_EMAIL_BLACKLISTED = "emailBlacklisted";
  private boolean smsBlackListed;
  private static final String JSON_PROPERTY_SMS_BLACKLISTED = "smsBlacklisted";
  private List<Long> listIds;
  private boolean updateEnabled;
  private static final String JSON_PROPERTY_UPDATE_ENABLED = "updateEnabled";

  private List<String> smtpBlackListed;
  private static final String JSON_PROPERTY_SMTP_BLACK_LISTED = "smtpBlacklisted";

  private Attributes attributes;
  public static final String JSON_PROPERTY_ATTRIBUTES = "attributes";


  @JsonProperty(JSON_PROPERTY_EMAIL)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public String getEmail() {
    return email;
  }

  @JsonProperty(JSON_PROPERTY_ATTRIBUTES)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public Attributes getAttributes() {
    return attributes;
  }

  @JsonProperty(JSON_PROPERTY_EMAIL_BLACKLISTED)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public boolean isEmailBlacklisted() {
    return emailBlackListed;
  }

  @JsonProperty(JSON_PROPERTY_SMS_BLACKLISTED)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public boolean isSmsBlacklisted() {
    return smsBlackListed;
  }

  @JsonProperty("listIds")
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public List<Long> getListIds() {
    return listIds;
  }

  @JsonProperty(JSON_PROPERTY_UPDATE_ENABLED)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public boolean isUpdateEnabled() {
    return updateEnabled;
  }

  @JsonProperty(JSON_PROPERTY_SMTP_BLACK_LISTED)
  @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
  public List<String> getSmtpBlacklisted() {
    return smtpBlackListed;
  }
}
