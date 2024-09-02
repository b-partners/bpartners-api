package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAreaPictureAnnotation;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaPictureAnnotationJpaRepository
    extends JpaRepository<HAreaPictureAnnotation, String> {
  List<HAreaPictureAnnotation> findAllByIdUserAndIdAreaPictureAndIsDraft(
      String idUser, String idAreaPicture, Boolean isDraft, Pageable pageable);
}
