package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.AreaPictureMapLayer;
import app.bpartners.api.model.mapper.AreaPictureMapLayerMapper;
import app.bpartners.api.repository.AreaPictureMapLayerRepository;
import app.bpartners.api.repository.jpa.AreaPictureMapLayerJpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AreaPictureMapLayerRepositoryImpl implements AreaPictureMapLayerRepository {
  private final AreaPictureMapLayerJpaRepository jpaRepository;
  private final AreaPictureMapLayerMapper mapper;

  @Override
  public Optional<AreaPictureMapLayer> findById(String id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public List<AreaPictureMapLayer> findAllByDepartementNameInIgnoreCaseOrderByYear(
      Collection<String> departementNames) {
    return jpaRepository.findAllByDepartementNameInIgnoreCase(departementNames).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
