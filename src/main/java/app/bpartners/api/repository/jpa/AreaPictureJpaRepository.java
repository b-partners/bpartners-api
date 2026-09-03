package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAreaPicture;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaPictureJpaRepository extends JpaRepository<HAreaPicture, String> {
  Optional<HAreaPicture> findByIdUserAndId(String idUser, String id);

  List<HAreaPicture> findAllByIdUser(String idUser);

  List<HAreaPicture> findAllByIdUserAndIdIn(String idUser, Collection<String> ids);

  List<HAreaPicture> findAllByIdUserAndAddressContainingIgnoreCase(String idUser, String address);

  List<HAreaPicture> findAllByIdUserAndAddressContainingIgnoreCaseAndFilenameContainingIgnoreCase(
      String idUser, String address, String filename);

  void deleteByIdProspect(String id);
}
