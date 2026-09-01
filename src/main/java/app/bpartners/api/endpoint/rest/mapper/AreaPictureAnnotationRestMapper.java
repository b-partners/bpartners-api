package app.bpartners.api.endpoint.rest.mapper;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.endpoint.rest.model.AreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.DraftAreaPictureAnnotation;
import app.bpartners.api.model.AreaPicture;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.service.areapicture.AreaPictureService;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureAnnotationRestMapper {
  private final AreaPictureAnnotationInstanceRestMapper instanceRestMapper;
  private final AreaPictureRestMapper areaPictureRestMapper;
  private final AreaPictureService areaPictureService;
  private final ProspectRepository prospectRepository;
  private final ProspectRestMapper prospectRestMapper;

  public AreaPictureAnnotation toRest(app.bpartners.api.model.AreaPictureAnnotation domain) {
    return new AreaPictureAnnotation()
        .id(domain.getId())
        .creationDatetime(domain.getCreationDatetime())
        .idAreaPicture(domain.getIdAreaPicture())
        .isDraft(domain.getIsDraft())
        .properties(domain.getProperties())
        .annotations(
            domain.getAnnotationInstances().stream()
                .map(instanceRestMapper::toRest)
                .collect(toUnmodifiableList()));
  }

  public app.bpartners.api.model.AreaPictureAnnotation toDomain(
      String id, String idUser, AreaPictureAnnotation rest) {
    if (!id.equals(rest.getId())) {
      throw new BadRequestException("payload id and path id aren't matching");
    }
    return app.bpartners.api.model.AreaPictureAnnotation.builder()
        .id(id)
        .idUser(idUser)
        .idAreaPicture(rest.getIdAreaPicture())
        .creationDatetime(rest.getCreationDatetime())
        .isDraft(rest.getIsDraft() != null && rest.getIsDraft())
        .properties(rest.getProperties())
        .annotationInstances(
            rest.getAnnotations().stream()
                .map(instanceRestMapper::toDomain)
                .collect(toUnmodifiableList()))
        .build();
  }

  public List<DraftAreaPictureAnnotation> toRestDrafts(
      String userId, List<app.bpartners.api.model.AreaPictureAnnotation> areaPictureAnnotations) {
    var idAreaPictures =
        areaPictureAnnotations.stream()
            .map(app.bpartners.api.model.AreaPictureAnnotation::getIdAreaPicture)
            .distinct()
            .toList();
    var areaPicturesById = findAllAreaPicturesByIdConcurrently(userId, idAreaPictures);

    var idProspects =
        areaPicturesById.values().stream()
            .map(AreaPicture::getIdProspect)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    var prospectsById =
        prospectRepository.findAllByIds(idProspects).stream()
            .collect(toMap(Prospect::getId, Function.identity()));

    return areaPictureAnnotations.stream()
        .map(
            areaPictureAnnotation -> {
              var restAnnotation = toRest(areaPictureAnnotation);
              var areaPicture = areaPicturesById.get(restAnnotation.getIdAreaPicture());
              var idProspect = areaPicture.getIdProspect();
              var prospect = idProspect == null ? null : prospectsById.get(idProspect);

              return new DraftAreaPictureAnnotation()
                  .id(restAnnotation.getId())
                  .isDraft(restAnnotation.getIsDraft())
                  .annotations(restAnnotation.getAnnotations())
                  .idAreaPicture(restAnnotation.getIdAreaPicture())
                  .properties(restAnnotation.getProperties())
                  .creationDatetime(restAnnotation.getCreationDatetime())
                  .areaPicture(areaPictureRestMapper.toRest(areaPicture))
                  .prospectName(prospect == null ? null : prospect.getName());
            })
        .toList();
  }

  @SneakyThrows
  private Map<String, AreaPicture> findAllAreaPicturesByIdConcurrently(
      String userId, List<String> idAreaPictures) {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      Map<String, Future<AreaPicture>> futuresById =
          idAreaPictures.stream()
              .collect(
                  toMap(
                      Function.identity(),
                      id -> executor.submit(() -> areaPictureService.findBy(userId, id))));
      return futuresById.entrySet().stream()
          .collect(toMap(Map.Entry::getKey, entry -> getUnchecked(entry.getValue())));
    }
  }

  @SneakyThrows
  private static AreaPicture getUnchecked(Future<AreaPicture> future) {
    try {
      return future.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
  }
}
