package app.bpartners.api.service.prospect;

import static app.bpartners.api.endpoint.rest.model.JobStatusValue.NOT_STARTED;

import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobStatus;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobType;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJobRunner;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProspectEvaluationJobConverter {
  public ProspectEvaluationJob convert(
      String accountHolderId, ProspectEvaluationJobRunner jobRunner) {
    return ProspectEvaluationJob.builder()
        .id(jobRunner.getJobId())
        .metadata(jobRunner.getMetadata())
        .idAccountHolder(accountHolderId)
        .type(getJobType(jobRunner))
        .jobStatus(new ProspectEvaluationJobStatus().value(NOT_STARTED).message(null))
        .startedAt(Instant.now())
        .endedAt(null)
        .results(List.of())
        .build();
  }

  private ProspectEvaluationJobType getJobType(ProspectEvaluationJobRunner job) {
    if (job.isEventConversionJob()) {
      return ProspectEvaluationJobType.CALENDAR_EVENT_CONVERSION;
    } else if (job.isSpreadsheetEvaluationJob()) {
      return ProspectEvaluationJobType.SPREADSHEET_EVALUATION;
    }
    throw new NotImplementedException(
        "Only prospect evaluation job type [CALENDAR_EVENT_CONVERSION and SPREADSHEET_EVALUATION]"
            + " are supported for now");
  }
}
