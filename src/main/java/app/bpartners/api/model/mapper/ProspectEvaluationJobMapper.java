package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobStatus;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HProspectEvaluationJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.StringUtils.toMetadataMap;

@Component
@AllArgsConstructor
public class ProspectEvaluationJobMapper {
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
  private final ProspectMapper prospectMapper;

  public ProspectEvaluationJob toDomain(HProspectEvaluationJob entity, List<HProspect> results) {
    return ProspectEvaluationJob.builder()
        .id(entity.getId())
        .idAccountHolder(entity.getIdAccountHolder())
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
        .metadata(toMetadataMap(entity.getMetadataString()))
        .build();
  }

  @SneakyThrows
  public HProspectEvaluationJob toEntity(ProspectEvaluationJob domain,
                                         List<HProspect> existingResults) {
    List<HProspect> actualResults = domain.getResults().stream()
        .map(prospect -> {
          Prospect.ProspectRating rating = prospect.getRating();
          return prospectMapper.toEntity(prospect,
              prospect.getIdHolderOwner(),
              rating.getValue(),
              rating.getLastEvaluationDate()); //No existing so creating a new one
        })
        .collect(Collectors.toList());
    List<HProspect> prospects = existingResults.isEmpty() ? actualResults
        : Stream.of(existingResults, actualResults)
        .flatMap(List::stream)
        .collect(Collectors.toList());
    return HProspectEvaluationJob.builder()
        .id(domain.getId())
        .idAccountHolder(domain.getIdAccountHolder())
        .type(domain.getType())
        .jobStatus(domain.getJobStatus().getValue())
        .jobStatusMessage(domain.getJobStatus().getMessage())
        .results(prospects)
        .startedAt(domain.getStartedAt())
        .endedAt(domain.getEndedAt())
        .metadataString(objectMapper.writeValueAsString(domain.getMetadata()))
        .build();
  }
}
