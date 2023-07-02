package app.bpartners.api.repository.expressif.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class InputValue<T> {
  @JsonProperty("Date")
  private Instant evaluationDate;
  @JsonProperty("Name")
  private String name;
  @JsonProperty("Value")
  private T value;
}
