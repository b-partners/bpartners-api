package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.model.ProspectEvaluationJob;
import java.util.List;

public interface ProspectEvaluationJobRepository {
  List<ProspectEvaluationJob> findAllByIdAccountHolderAndStatusesIn(String ahId,
                                                                    List<JobStatusValue> statuses);
}
