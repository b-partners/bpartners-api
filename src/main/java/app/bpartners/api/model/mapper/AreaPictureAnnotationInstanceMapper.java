package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AreaPictureAnnotationInstance;
import app.bpartners.api.model.validator.AreaPictureAnnotationInstanceValidator;
import app.bpartners.api.model.validator.PolygonValidator;
import app.bpartners.api.repository.jpa.model.HAreaPictureAnnotationInstance;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AreaPictureAnnotationInstanceMapper {
  private final AreaPictureAnnotationInstanceValidator validator;
  private final PolygonValidator polygonValidator;

  public AreaPictureAnnotationInstance toDomain(HAreaPictureAnnotationInstance entity) {
    AreaPictureAnnotationInstance.Metadata metadata =
        AreaPictureAnnotationInstance.Metadata.builder()
            .area(entity.getArea())
            .slope(entity.getSlope())
            .covering(entity.getCovering())
            .wearLevel(entity.getWearLevel())
            .build();
    return AreaPictureAnnotationInstance.builder()
        .id(entity.getId())
        .metadata(metadata)
        .polygon(entity.getPolygon())
        .labelName(entity.getLabelName())
        .idAnnotation(entity.getIdAnnotation())
        .idUser(entity.getIdUser())
        .idAreaPicture(entity.getIdAreaPicture())
        .build();
  }

  public HAreaPictureAnnotationInstance toEntity(AreaPictureAnnotationInstance domain) {
    validator.accept(domain);
    polygonValidator.accept(domain.getPolygon());
    AreaPictureAnnotationInstance.Metadata metadata = domain.getMetadata();
    return HAreaPictureAnnotationInstance.builder()
        .id(domain.getId())
        .slope(metadata.slope())
        .area(metadata.area())
        .covering(metadata.covering())
        .wearLevel(metadata.wearLevel())
        .polygon(domain.getPolygon())
        .labelName(domain.getLabelName())
        .idAnnotation(domain.getIdAnnotation())
        .idUser(domain.getIdUser())
        .idAreaPicture(domain.getIdAreaPicture())
        .build();
  }
}
