package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.JobStatusValue;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.ProspectEvaluationJobMapper;
import app.bpartners.api.repository.ProspectEvaluationJobRepository;
import app.bpartners.api.repository.jpa.ProspectEvaluationJobJpaRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HProspectEvaluationJob;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class ProspectEvaluationJobRepositoryImpl implements ProspectEvaluationJobRepository {
  private final ProspectEvaluationJobJpaRepository jobJpaRepository;
  private final ProspectJpaRepository prospectJpaRepository;
  private final ProspectEvaluationJobMapper mapper;

  @Override
  public List<ProspectEvaluationJob> findAllByIdAccountHolderAndStatusesIn(String ahId,
                                                                           List<JobStatusValue> statuses) {
    return jobJpaRepository.findAllByIdAccountHolderAndJobStatusInOrderByStartedAtDesc(
            ahId, statuses).stream()
        .map(entity -> {
          List<HProspect> results = prospectJpaRepository.findAllByIdJob(entity.getId());
          return mapper.toDomain(entity, results);
        })
        .collect(Collectors.toList());
  }

  @Override
  public ProspectEvaluationJob getById(String id) {
    HProspectEvaluationJob entity = jobJpaRepository.findById(id)
        .orElseThrow(
            () -> new NotFoundException("Job(id=" + id + ") not found"));
    List<HProspect> results = prospectJpaRepository.findAllByIdJob(entity.getId());
    return mapper.toDomain(entity, results);
  }

  @Override
  public List<ProspectEvaluationJob> saveAll(List<ProspectEvaluationJob> toSave) {
    List<HProspectEvaluationJob> jobEntities = toSave.stream()
        .map(mapper::toEntity)
        .collect(Collectors.toList());
    return jobJpaRepository.saveAll(jobEntities).stream()
        .map(entity -> {
          List<HProspect> results = prospectJpaRepository.findAllByIdJob(entity.getId());
          return mapper.toDomain(entity, results);
        })
        .collect(Collectors.toList());
  }
}
