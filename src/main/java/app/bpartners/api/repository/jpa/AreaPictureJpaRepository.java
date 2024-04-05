package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAreaPicture;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AreaPictureJpaRepository extends JpaRepository<HAreaPicture, String> {
  Optional<HAreaPicture> findByIdUserAndId(String idUser, String id);

  List<HAreaPicture> findAllByIdUserAndAddressContainingIgnoreCaseAndFilenameContainingIgnoreCase(
      String idUser, String address, String filename);
}
