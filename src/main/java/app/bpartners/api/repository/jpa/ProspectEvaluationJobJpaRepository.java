package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.repository.jpa.model.HProspectEvaluationJob;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProspectEvaluationJobJpaRepository
    extends JpaRepository<HProspectEvaluationJob, String> {

  List<HProspectEvaluationJob> findAllByIdAccountHolderAndJobStatusInOrderByStartedAtDesc(String ahId,
                                                                      List<JobStatusValue> statuses);

}
