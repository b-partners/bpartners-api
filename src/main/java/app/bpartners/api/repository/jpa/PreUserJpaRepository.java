package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.entity.HPreUser;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PreUserJpaRepository extends JpaRepository<HPreUser, String> {
  @Query("select h from HPreUser h where (:firstname is null or lower(h.firstName) like lower"
      + "(concat('%',:firstname,'%') ) ) and "
      + "(:lastname is null or lower(h.lastName) like lower(concat('%',:lastname,'%') ) ) and "
      + "(:society is null or lower(h.society) like lower(concat('%',:society,'%') ) ) and"
      + "(:email is null or lower(h.email) like lower(concat('%',:email,'%') ) )")
  List<HPreUser> getByCriteria(
      Pageable pageable,
      @Param("firstname") String firstName,
      @Param("lastname") String lastName,
      @Param("society") String society,
      @Param("email") String email
  );

}
