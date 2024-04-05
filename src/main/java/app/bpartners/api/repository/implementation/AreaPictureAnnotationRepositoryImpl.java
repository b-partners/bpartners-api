package app.bpartners.api.repository.implementation;

import static java.util.stream.Collectors.toUnmodifiableList;

import app.bpartners.api.model.AreaPictureAnnotation;
import app.bpartners.api.model.mapper.AreaPictureAnnotationMapper;
import app.bpartners.api.repository.AreaPictureAnnotationRepository;
import app.bpartners.api.repository.jpa.AreaPictureAnnotationJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AreaPictureAnnotationRepositoryImpl implements AreaPictureAnnotationRepository {
  private final AreaPictureAnnotationJpaRepository jpaRepository;
  private final AreaPictureAnnotationMapper mapper;

  @Override
  public List<AreaPictureAnnotation> findAllBy(
      String idUser, String idAreaPicture, Pageable pageable) {
    return jpaRepository.findAllByIdUserAndIdAreaPicture(idUser, idAreaPicture, pageable).stream()
        .map(mapper::toDomain)
        .collect(toUnmodifiableList());
  }

  @Override
  public Optional<AreaPictureAnnotation> findBy(String idUser, String idAreaPicture, String id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public AreaPictureAnnotation save(AreaPictureAnnotation areaPictureAnnotation) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(areaPictureAnnotation)));
  }
}
