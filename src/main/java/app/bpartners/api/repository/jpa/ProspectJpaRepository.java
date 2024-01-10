package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.repository.jpa.model.HProspect;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProspectJpaRepository extends JpaRepository<HProspect, String> {
  // TODO: why do prospects must be filtered by town code
  // while it is already attached to account holder ?
  List<HProspect> findAllByIdAccountHolderAndTownCodeIsIn(
      String idAccountHolder, List<Integer> townCode);

  List<HProspect> findAllByIdAccountHolder(String idAccountHolder);

  List<HProspect> findAllByIdAccountHolderAndOldNameContainingIgnoreCase(
      String idAccountHolder, String name);

  List<HProspect> findAllByIdAccountHolderAndOldNameContainingIgnoreCaseAndContactNature(
      String idAccountHolder, String name, ContactNature contactNature);

  List<HProspect> findAllByIdJob(String idJob);

  @Query(
      nativeQuery = true,
      value =
          "select id,"
              + "       old_name,"
              + "       old_email,"
              + "       old_phone,"
              + "       old_address,"
              + "       id_account_holder,"
              + "       town_code,"
              + "       rating,"
              + "       last_evaluation_date,"
              + "       id_prospect_eval,"
              + "       pos_latitude,"
              + "       pos_longitude,"
              + "       new_name,"
              + "       new_email,"
              + "       new_phone,"
              + "       new_address,"
              + "       comment,"
              + "       contract_amount,"
              + "       id_invoice,"
              + "       prospect_feedback,"
              + "       id_job,"
              + "       default_comment,"
              + "       manager_name,"
              + "       contact_nature,"
              + "       latest_old_holder"
              + " from view_prospect_actual_status"
              + " where cast(actual_status as varchar) = ?1")
  List<HProspect> findAllByStatus(String prospectStatus);
}
