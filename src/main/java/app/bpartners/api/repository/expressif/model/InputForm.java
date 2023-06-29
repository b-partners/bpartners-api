package app.bpartners.api.repository.expressif.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
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
public class InputForm {
  @JsonProperty("EvaluationDate")
  private Instant evaluationDate;
  @JsonProperty("InputValues")
  private List<InputValue> inputValues;
}
