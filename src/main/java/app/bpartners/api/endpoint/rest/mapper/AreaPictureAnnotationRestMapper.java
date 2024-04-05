package app.bpartners.api.endpoint.rest.mapper;

import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.endpoint.rest.model.AreaPictureAnnotation;
import app.bpartners.api.model.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureAnnotationRestMapper {
  private final AreaPictureAnnotationInstanceRestMapper instanceRestMapper;

  public AreaPictureAnnotation toRest(app.bpartners.api.model.AreaPictureAnnotation domain) {
    return new AreaPictureAnnotation()
        .id(domain.getId())
        .creationDatetime(domain.getCreationDatetime())
        .idAreaPicture(domain.getIdAreaPicture())
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
        .annotationInstances(
            rest.getAnnotations().stream()
                .map(instanceRestMapper::toDomain)
                .collect(toUnmodifiableList()))
        .build();
  }
}
