package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.ProspectFeedback;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.model.prospect.ProspectStatusHistory;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.expressif.ProspectEvaluation;
import app.bpartners.api.repository.expressif.ProspectEvaluationInfo;
import app.bpartners.api.repository.expressif.ProspectResult;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HProspectStatusHistory;
import app.bpartners.api.service.CustomerService;
import app.bpartners.api.service.utils.GeoUtils;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static app.bpartners.api.model.prospect.ProspectStatusHistory.defaultStatusHistory;
import static app.bpartners.api.repository.jpa.model.HProspectStatusHistory.defaultStatusHistoryEntity;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.util.UUID.randomUUID;

@Component
@Slf4j
public class ProspectMapper {
  private static String checkIfOldOrNew(String toCheck,
                                        String expected) {
    if (toCheck != null && expected != null) {
      return !toCheck.equals(expected) ? toCheck : null;
    } else if (expected == null && toCheck != null) {
      return toCheck;
    }
    return expected;
  }

  private static String isNewExists(String toCheck, String actual) {
    return toCheck != null ? toCheck : actual;
  }

  public HProspect toEntity(Prospect domain, HProspect existing) {
    Double rating = existing == null || existing.getRating() == null ? -1 : existing.getRating();
    Instant lastEvaluationDate = existing == null ? null : existing.getLastEvaluationDate();
    return toEntity(domain,
        domain.getIdHolderOwner(),
        rating,
        lastEvaluationDate,
        existing);
  }

  //TODO: put this constraint check inside service not here
  public HProspect toEntity(Prospect domain,
                            String prospectOwnerId,
                            Double rating,
                            Instant lastEvaluationDate,
                            HProspect existingEntity) {
    Geojson location = domain.getLocation();
    HProspect entity = existingEntity == null ? HProspect.builder()
        .id(String.valueOf(randomUUID()))
        .idAccountHolder(prospectOwnerId)
        .statusHistories(defaultStatusHistoryEntity())
        .build() : existingEntity;
    List<HProspectStatusHistory> actualHistory = entity.getStatusHistories();

    boolean prospectIsResetToContact = existingEntity != null
        && existingEntity.getActualStatus() != TO_CONTACT
        && domain.getActualStatus().equals(TO_CONTACT);

    boolean prospectIsDeclined = domain.getProspectFeedback() != null
        && domain.getProspectFeedback() == ProspectFeedback.NOT_INTERESTED
        || domain.getProspectFeedback() == ProspectFeedback.PROPOSAL_DECLINED;

    if (prospectIsResetToContact || prospectIsDeclined) {
      return existingEntity.toBuilder()
          .newName(null)
          .newEmail(null)
          .newPhone(null)
          .newAddress(null)
          .idAccountHolder(null)
          .comment(null)
          .contractAmount(null)
          .prospectFeedback(null)
          .idInvoice(null)
          .latestOldHolder(domain.getIdHolderOwner())
          .statusHistories(updatedStatusHistory(actualHistory, defaultStatusHistoryEntity()))
          .build();
    } else {
      List<HProspectStatusHistory> newStatusHistory = List.of(
          HProspectStatusHistory.builder()
              .id(String.valueOf(randomUUID()))
              .status(domain.getActualStatus())
              .updatedAt(Instant.now())
              .build());
      return entity.toBuilder()
          .id(domain.getId())
          .idJob(domain.getIdJob())
          .managerName(domain.getManagerName())
          .oldPhone(existingEntity == null ? domain.getPhone()
              : existingEntity.getOldPhone())
          .oldName(existingEntity == null ? domain.getName()
              : existingEntity.getOldName())
          .oldAddress(existingEntity == null ? domain.getAddress()
              : existingEntity.getOldAddress())
          .oldEmail(existingEntity == null ? domain.getEmail()
              : existingEntity.getOldEmail())
          .newPhone(existingEntity == null ? null
              : checkIfOldOrNew(domain.getPhone(), entity.getOldPhone()))
          .newName(existingEntity == null ? null
              : checkIfOldOrNew(domain.getName(), entity.getOldName()))
          .newAddress(existingEntity == null ? null
              : checkIfOldOrNew(domain.getAddress(), entity.getOldAddress()))
          .newEmail(existingEntity == null ? null
              : checkIfOldOrNew(domain.getEmail(), entity.getOldEmail()))
          .statusHistories(updatedStatusHistory(actualHistory, newStatusHistory))
          .idAccountHolder(prospectOwnerId)
          .townCode(domain.getTownCode())
          .rating(rating)
          .lastEvaluationDate(lastEvaluationDate)
          .posLongitude(location == null ? null : location.getLongitude())
          .posLatitude(location == null ? null : location.getLatitude())
          .comment(domain.getComment())
          .defaultComment(domain.getDefaultComment())
          .idInvoice(domain.getIdInvoice())
          .prospectFeedback(domain.getProspectFeedback())
          .contactNature(existingEntity == null
              ? (domain.getContactNature() == null ? ContactNature.PROSPECT :
              domain.getContactNature())
              : existingEntity.getContactNature())
          .contractAmount(
              domain.getContractAmount() == null ? null : domain.getContractAmount().toString())
          .build();
    }
  }

  public HProspect toEntity(Prospect domain,
                            String prospectOwnerId,
                            Double rating,
                            Instant lastEvaluationDate) {
    List<HProspectStatusHistory> newStatusHistory = List.of(
        HProspectStatusHistory.builder()
            .id(String.valueOf(randomUUID()))
            .status(domain.getActualStatus())
            .updatedAt(Instant.now())
            .build());
    Geojson location = domain.getLocation();
    return HProspect.builder()
        .id(domain.getId())
        .oldPhone(domain.getPhone())
        .managerName(domain.getManagerName())
        .oldName(domain.getName())
        .oldEmail(domain.getEmail())
        .statusHistories(updatedStatusHistory(List.of(), newStatusHistory))
        .oldAddress(domain.getAddress())
        .idAccountHolder(prospectOwnerId)
        .townCode(domain.getTownCode())
        .rating(rating)
        .lastEvaluationDate(lastEvaluationDate)
        .posLongitude(location == null ? null : location.getLongitude())
        .posLatitude(location == null ? null : location.getLatitude())
        .defaultComment(domain.getDefaultComment())
        .contactNature(domain.getContactNature())
        .build();
  }

  public Prospect toDomain(HProspect entity, Geojson location) {
    return Prospect.builder()
        .id(entity.getId())
        .idJob(entity.getIdJob())
        .idHolderOwner(entity.getIdAccountHolder())
        .email(isNewExists(entity.getNewEmail(), entity.getOldEmail()))
        .address(isNewExists(entity.getNewAddress(), entity.getOldAddress()))
        .name(isNewExists(entity.getNewName(), entity.getOldName()))
        .managerName(entity.getManagerName())
        .phone(isNewExists(entity.getNewPhone(), entity.getOldPhone()))
        .location(location)
        .statusHistories(entity.getStatusHistories().stream()
            .map(this::toDomain)
            .collect(Collectors.toList()))
        .townCode(entity.getTownCode())
        .rating(Prospect.ProspectRating.builder()
            .value(entity.getRating())
            .lastEvaluationDate(entity.getLastEvaluationDate())
            .build())
        .comment(entity.getComment())
        .defaultComment(entity.getDefaultComment())
        .prospectFeedback(entity.getProspectFeedback())
        .idInvoice(entity.getIdInvoice())
        .contractAmount(entity.getContractAmount() == null
            ? null
            : parseFraction(entity.getContractAmount()))
        .contactNature(entity.getContactNature())
        .latestOldHolder(entity.getLatestOldHolder())
        .build();
  }

  public ProspectStatusHistory toDomain(HProspectStatusHistory history) {
    return ProspectStatusHistory.builder()
        .status(history.getStatus())
        .updatedAt(history.getUpdatedAt())
        .build();
  }

  private List<HProspectStatusHistory> updatedStatusHistory(
      List<HProspectStatusHistory> actualHistory, List<HProspectStatusHistory> newStatusHistory) {
    return Stream.of(actualHistory, newStatusHistory)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  public Prospect fromEvaluationInfos(ProspectResult result,
                                      ProspectEvaluation prospectEvaluation,
                                      ProspectEvaluationInfo info,
                                      GeoUtils.Coordinate coordinates) {
    Integer townCode;
    try {
      townCode =
          info == null ? null : Integer.valueOf(info.getPostalCode());
    } catch (NumberFormatException e) {
      townCode = null;
    }
    return Prospect.builder()
        //TODO: change when prospect prospectEvaluation can be override
        .id(String.valueOf(randomUUID()))
        .idHolderOwner(prospectEvaluation == null ? null
            : prospectEvaluation.getProspectOwnerId())
        .name(info == null ? null : info.getName())
        .managerName(info == null ? null : info.getManagerName())
        .email(info == null ? null : info.getEmail())
        .phone(info == null ? null : info.getPhoneNumber())
        .address(info == null ? null : info.getAddress())
        .statusHistories(defaultStatusHistory())
        .townCode(info == null ? null : townCode)
        .defaultComment(info == null ? null
            : info.getDefaultComment())
        .townCode(info == null ? null : Integer.valueOf(info.getPostalCode()))
        .location(new Geojson()
            .latitude(coordinates == null ? null
                : coordinates.getLatitude())
            .longitude(coordinates == null ? null
                : coordinates.getLongitude()))
        .rating(Prospect.ProspectRating.builder()
            .value(result == null ? null : result.getInterventionResult().getRating())
            .lastEvaluationDate(result == null ? null : result.getEvaluationDate())
            .build())
        .build();
  }

  public List<Prospect> toNewProspect(List<ProspectResult> prospectResults,
                                      Double minProspectRating) {
    return prospectResults.stream()
        .map(this::toNewProsect)
        .collect(Collectors.toList());
  }

  public Prospect toNewProsect(ProspectResult result) {
    ProspectEvaluation prospectEvaluation = result.getProspectEvaluation();
    ProspectEvaluationInfo info = prospectEvaluation.getProspectEvaluationInfo();
    GeoUtils.Coordinate coordinates = info.getCoordinates();

    return fromEvaluationInfos(result, prospectEvaluation, info, coordinates);
  }

  public List<Prospect> toCustomerProspect(List<ProspectResult> prospectResults,
                                           CustomerService customerService) {
    HashMap<String, List<ProspectResult>> groupByCustomer =
        dispatchResultByCustomer(prospectResults);
    List<Prospect> prospects = new ArrayList<>();
    for (Map.Entry<String, List<ProspectResult>> entry : groupByCustomer.entrySet()) {
      String idCustomer = entry.getKey();
      if (idCustomer != null) {
        Customer customer = customerService.getCustomerById(idCustomer);
        List<ProspectResult> subList = entry.getValue();
        for (ProspectResult result : subList) {
          prospects.add(toCustomerProspect(result, customer));
        }
      } else {
        log.info("Prospects results were customer null {}", entry.getValue());
      }
    }
    return prospects;
  }

  public Prospect toCustomerProspect(ProspectResult result, Customer customer) {
    ProspectEvaluation eval = result.getProspectEvaluation();
    result.getCustomerInterventionResult().setOldCustomer(customer);
    return Prospect.builder()
        .id(String.valueOf(randomUUID())) //TODO: change when prospect eval can be override
        .idHolderOwner(eval.getProspectOwnerId())
        .name(customer.getName())
        .managerName(customer.getName())
        .email(customer.getEmail())
        .phone(customer.getPhone())
        .address(customer.getFullAddress())
        .statusHistories(defaultStatusHistory())
        .townCode(Integer.valueOf(customer.getZipCode()))
        .location(new Geojson()
            .latitude(customer.getLocation().getCoordinate().getLatitude())
            .longitude(customer.getLocation().getCoordinate().getLongitude()))
        .rating(Prospect.ProspectRating.builder()
            .value(result.getInterventionResult().getRating())
            .lastEvaluationDate(result.getEvaluationDate())
            .build())
        .build();
  }

  private HashMap<String, List<ProspectResult>> dispatchResultByCustomer(
      List<ProspectResult> prospects) {
    HashMap<String, List<ProspectResult>> prospectResultMap = new HashMap<>();
    for (ProspectResult result : prospects) {
      String idCustomer = result.getCustomerInterventionResult().getIdCustomer();
      if (idCustomer != null) {
        if (!prospectResultMap.containsKey(idCustomer)) {
          List<ProspectResult> subList = new ArrayList<>();
          subList.add(result);
          prospectResultMap.put(idCustomer, subList);
        } else {
          prospectResultMap.get(idCustomer).add(result);
        }
      }
    }
    return prospectResultMap;
  }
}
