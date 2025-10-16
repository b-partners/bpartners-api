package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.repository.jpa.model.HProspect;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProspectJpaRepository extends JpaRepository<HProspect, String> {
  // TODO: why do prospects must be filtered by town code
  // while it is already attached to account holder ?
  List<HProspect> findAllByIdAccountHolderAndTownCodeIsIn(
      String idAccountHolder, List<Integer> townCode);

  List<HProspect> findAllByIdAccountHolder(String idAccountHolder, Pageable pageable);

  List<HProspect> findAllByIdAccountHolderAndOldNameContainingIgnoreCase(
      String idAccountHolder, String name, Pageable pageable);

  List<HProspect> findAllByIdAccountHolderAndOldNameContainingIgnoreCaseAndContactNature(
      String idAccountHolder, String name, ContactNature contactNature, Pageable pageable);

  @Query(
      nativeQuery = true,
      value =
          "select id,       first_name,       old_name,       old_email,       old_phone,      "
              + " old_address,       id_account_holder,       town_code,       rating,      "
              + " last_evaluation_date,       id_prospect_eval,       pos_latitude,      "
              + " pos_longitude,       new_name,       new_email,       new_phone,      "
              + " new_address,       comment,       contract_amount,       id_invoice,      "
              + " prospect_feedback,       id_job,       default_comment,       manager_name,      "
              + " contact_nature,       latest_old_holder, creation_datetime, update_datetime  from"
              + " view_prospect_actual_status  where id_account_holder = ?1  and (old_name ILIKE"
              + " concat('%', ?3, '%') or new_name ILIKE concat('%', ?3, '%'))  and"
              + " cast(contact_nature as varchar) = ?3  and cast(actual_status as varchar) = ?4"
              + " ORDER BY creation_datetime DESC NULLS LAST , update_datetime DESC NULLS LAST ,"
              + " status_updated_at DESC LIMIT ?5 OFFSET ?6")
  List<HProspect>
      findAllByIdAccountHolderAndOldNameContainingIgnoreCaseAndContactNatureAndPropsectStatusOrderByCreationDatetimeDesc(
          String idAccountHolder,
          String name,
          String contactNature,
          String prospectStatus,
          int pageSize,
          int page);

  @Query(
      nativeQuery = true,
      value =
          "SELECT id,       first_name,       old_name,       old_email,       old_phone,      "
              + " old_address,       id_account_holder,       town_code,       rating,      "
              + " last_evaluation_date,       id_prospect_eval,       pos_latitude,      "
              + " pos_longitude,       new_name,       new_email,       new_phone,      "
              + " new_address,       comment,       contract_amount,       id_invoice,      "
              + " prospect_feedback,       id_job,       default_comment,       manager_name,      "
              + " contact_nature,       latest_old_holder, creation_datetime, update_datetime FROM"
              + " view_prospect_actual_status WHERE CAST(actual_status AS VARCHAR)=?1 and"
              + " id_account_holder=?2 and (old_name ILIKE concat('%', ?3, '%') or new_name ILIKE"
              + " concat('%', ?3, '%')) ORDER BY creation_datetime DESC NULLS LAST ,"
              + " update_datetime DESC NULLS  LAST, status_updated_at DESC LIMIT ?4 OFFSET ?5")
  List<HProspect> findAllByIdAccountHolderAndOldNameAndProspectStatusOrderByCreationDatetimeDesc(
      String prospectStatus, String idAccountHolder, String name, int pageSize, int page);

  List<HProspect> findAllByIdJob(String idJob);

  @Query(
      nativeQuery = true,
      value =
          "select id,"
              + "       first_name,"
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
              + "       latest_old_holder,"
              + "       creation_datetime,"
              + "       update_datetime"
              + " from view_prospect_actual_status"
              + " where cast(actual_status as varchar) = ?1")
  List<HProspect> findAllByStatus(String prospectStatus);
}
