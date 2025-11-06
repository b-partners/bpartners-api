package app.bpartners.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoogleCaptchaResponse {
  private boolean success;
  private Double score;

  @JsonProperty("error-codes")
  private List<String> errorCodes;
}
