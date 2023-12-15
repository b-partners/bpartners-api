package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import java.util.List;

public interface ProspectEvaluationJobRepository {
  List<ProspectEvaluationJob> findAllByIdAccountHolderAndStatusesIn(
      String ahId, List<JobStatusValue> statuses);

  ProspectEvaluationJob getById(String id);

  List<ProspectEvaluationJob> saveAll(List<ProspectEvaluationJob> toSave);
}
