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
public class OutputValue<T> {
  @JsonProperty("date")
  private Instant evaluationDate;
  @JsonProperty("name")
  private String name;
  @JsonProperty("value")
  private T value;
}
