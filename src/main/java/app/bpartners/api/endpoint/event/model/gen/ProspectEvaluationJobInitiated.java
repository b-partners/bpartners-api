package app.bpartners.api.endpoint.event.model.gen;

import app.bpartners.api.model.prospect.job.ProspectEvaluationJobRunner;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

//TODO: use generated from EventBridge instead
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ProspectEvaluationJobInitiated implements Serializable {
  private String jobId;
  private String idUser;
  private ProspectEvaluationJobRunner jobRunner;
}
