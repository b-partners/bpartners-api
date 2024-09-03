package app.bpartners.api.endpoint.rest.mapper;

import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.endpoint.rest.model.AreaPictureAnnotation;
import app.bpartners.api.endpoint.rest.model.DraftAreaPictureAnnotation;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.AreaPictureService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureAnnotationRestMapper {
  private final AreaPictureAnnotationInstanceRestMapper instanceRestMapper;
  private final AreaPictureRestMapper areaPictureRestMapper;
  private final AreaPictureService areaPictureService;

  public AreaPictureAnnotation toRest(app.bpartners.api.model.AreaPictureAnnotation domain) {
    return new AreaPictureAnnotation()
        .id(domain.getId())
        .creationDatetime(domain.getCreationDatetime())
        .idAreaPicture(domain.getIdAreaPicture())
        .isDraft(domain.getIsDraft())
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
        .isDraft(rest.getIsDraft())
        .annotationInstances(
            rest.getAnnotations().stream()
                .map(instanceRestMapper::toDomain)
                .collect(toUnmodifiableList()))
        .build();
  }

  public DraftAreaPictureAnnotation toRestDraft(
      String userId, app.bpartners.api.model.AreaPictureAnnotation areaPictureAnnotation) {
    var restAnnotation = toRest(areaPictureAnnotation);
    var areaPicture = areaPictureService.findBy(userId, restAnnotation.getIdAreaPicture());

    return new DraftAreaPictureAnnotation()
        .id(restAnnotation.getId())
        .isDraft(restAnnotation.getIsDraft())
        .annotations(restAnnotation.getAnnotations())
        .idAreaPicture(restAnnotation.getIdAreaPicture())
        .creationDatetime(restAnnotation.getCreationDatetime())
        .areaPicture(areaPictureRestMapper.toRest(areaPicture));
  }
}
