package app.bpartners.api.repository;

import app.bpartners.api.model.AreaPictureAnnotation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface AreaPictureAnnotationRepository {
  List<AreaPictureAnnotation> findAllBy(String idUser, String idAreaPicture, Boolean isDraft, Pageable pageable);

  Optional<AreaPictureAnnotation> findBy(String idUser, String idAreaPicture, String id);

  AreaPictureAnnotation save(AreaPictureAnnotation areaPictureAnnotation);
}
