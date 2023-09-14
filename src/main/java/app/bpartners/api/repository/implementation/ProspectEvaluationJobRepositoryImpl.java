package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.model.ProspectEvaluationJob;
import app.bpartners.api.model.mapper.ProspectEvaluationJobMapper;
import app.bpartners.api.repository.ProspectEvaluationJobRepository;
import app.bpartners.api.repository.jpa.ProspectEvaluationJobJpaRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class ProspectEvaluationJobRepositoryImpl implements ProspectEvaluationJobRepository {
  private final ProspectEvaluationJobJpaRepository jpaRepository;
  private final ProspectEvaluationJobMapper mapper;

  @Override
  public List<ProspectEvaluationJob> findAllByIdAccountHolderAndStatusesIn(String ahId,
                                                                           List<JobStatusValue> statuses) {
    return jpaRepository.findAllByIdAccountHolderAndJobStatusIn(ahId, statuses).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }
}
