package app.bpartners.api.repository;

import app.bpartners.api.model.AreaPictureMapLayer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface AreaPictureMapLayerRepository {
  Optional<AreaPictureMapLayer> findById(String id);

  List<AreaPictureMapLayer> findAllByDepartementNameInIgnoreCaseOrderByYear(
      Collection<String> departementName);

  List<AreaPictureMapLayer> findAll(Pageable pageable);
}
