package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.PreUser;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PreUserJpaRepository extends JpaRepository<PreUser, String> {
  @Query
    (
      "SELECT p FROM PreUser p WHERE lower(p.firstName) LIKE lower(concat('%',:firstname,'%') ) " +
      "AND lower(p.lastName) LIKE lower(concat('%',:lastname,'%') ) " +
      "AND lower(p.society) LIKE lower(concat('%',:society,'%') ) " +
      "AND lower(p.email) LIKE lower(concat('%',:email,'%') )"
    )
    List<PreUser> getByCriteria(
    @Param("firstname") String firstName,
    @Param("lastname") String lastName,
    @Param("society") String society,
    @Param ("email") String email,
    Pageable pageable
  );
  List<PreUser> findAllByFirstNameContainingIgnoreCaseAndLastNameContainingIgnoreCaseAndSocietyContainingIgnoreCaseAndEmailContainingIgnoreCase(
    String firstName, String lastName, String society, String email, Pageable pageable);
}
