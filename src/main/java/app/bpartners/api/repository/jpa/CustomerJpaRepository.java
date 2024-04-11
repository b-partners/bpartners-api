package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HCustomer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

public interface CustomerJpaRepository extends JpaRepository<HCustomer, String> {

  List<HCustomer> findAllByIdUserOrderByLastNameAsc(String idUser);

  List<HCustomer> findAllByLatitudeIsNullOrLongitudeIsNull();

  @Query(
      "select c from HCustomer c join HUser u on c.idUser = u.id"
          + " join HAccountHolder a on a.idUser = u.id"
          + " where a.id = :accountHolderId")
  List<HCustomer> findAllByIdAccountHolder(@Param("accountHolderId") String accountHolderId);

  @Query("select c from HCustomer c where c.prospect.id = ?1")
  Optional<HCustomer> findByIdProspect(@NonNull String idProspect);
}
