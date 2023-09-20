package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobStatus;
import app.bpartners.api.model.ProspectEvaluationJob;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HProspectEvaluationJob;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ProspectEvaluationJobMapper {
  private final ProspectMapper prospectMapper;

  public ProspectEvaluationJob toDomain(HProspectEvaluationJob entity, List<HProspect> results) {
    return ProspectEvaluationJob.builder()
        .id(entity.getId())
        .type(entity.getType())
        .jobStatus(new ProspectEvaluationJobStatus()
            .value(entity.getJobStatus())
            .message(entity.getJobStatusMessage()))
        .startedAt(entity.getStartedAt())
        .endedAt(entity.getEndedAt())
        .results(results.stream()
            .map(prospect -> {
              Geojson location =
                  prospect.getPosLatitude() != null && prospect.getPosLongitude() != null
                      ? null : new Geojson()
                      .latitude(prospect.getPosLatitude())
                      .longitude(prospect.getPosLongitude());
              return prospectMapper.toDomain(prospect, location);
            })
            .collect(Collectors.toList()))
        .build();
  }

}
