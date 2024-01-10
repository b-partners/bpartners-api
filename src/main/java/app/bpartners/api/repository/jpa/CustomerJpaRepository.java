package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HCustomer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerJpaRepository extends JpaRepository<HCustomer, String> {

  List<HCustomer> findAllByIdUserOrderByLastNameAsc(String idUser);

  List<HCustomer> findAllByLatitudeIsNullOrLongitudeIsNull();

  @Query(
      "select c from HCustomer c join HUser u on c.idUser = u.id"
          + " join HAccountHolder a on a.idUser = u.id"
          + " where a.id = :accountHolderId")
  List<HCustomer> findAllByIdAccountHolder(@Param("accountHolderId") String accountHolderId);
}
