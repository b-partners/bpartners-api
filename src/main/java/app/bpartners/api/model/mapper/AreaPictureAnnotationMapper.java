package app.bpartners.api.model.mapper;

import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.model.AreaPictureAnnotation;
import app.bpartners.api.repository.jpa.model.HAreaPictureAnnotation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureAnnotationMapper {
  private final AreaPictureAnnotationInstanceMapper instanceMapper;

  public AreaPictureAnnotation toDomain(HAreaPictureAnnotation entity) {
    return AreaPictureAnnotation.builder()
        .id(entity.getId())
        .creationDatetime(entity.getCreationDatetime())
        .idUser(entity.getIdUser())
        .isDraft(entity.getIsDraft())
        .idAreaPicture(entity.getIdAreaPicture())
        .annotationInstances(
            entity.getAnnotationInstances().stream()
                .map(instanceMapper::toDomain)
                .collect(toUnmodifiableList()))
        .build();
  }

  public HAreaPictureAnnotation toEntity(AreaPictureAnnotation domain) {
    return HAreaPictureAnnotation.builder()
        .id(domain.getId())
        .creationDatetime(domain.getCreationDatetime())
        .idUser(domain.getIdUser())
        .isDraft(domain.getIsDraft())
        .idAreaPicture(domain.getIdAreaPicture())
        .annotationInstances(
            domain.getAnnotationInstances().stream()
                .map(instanceMapper::toEntity)
                .collect(toUnmodifiableList()))
        .build();
  }
}
