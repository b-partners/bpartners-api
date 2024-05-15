package app.bpartners.api.repository;

import app.bpartners.api.model.AreaPictureMapLayer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AreaPictureMapLayerRepository {
  Optional<AreaPictureMapLayer> findById(String id);

  List<AreaPictureMapLayer> findAllByDepartementNameInIgnoreCaseOrderByYear(
      Collection<String> departementName);

  Optional<AreaPictureMapLayer> findByDepartementNameIgnoreCaseAndYear(
      String departementName, int year);
}
