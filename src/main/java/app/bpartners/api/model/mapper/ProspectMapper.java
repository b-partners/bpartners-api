package app.bpartners.api.model.mapper;

import static app.bpartners.api.endpoint.rest.model.ProspectStatus.TO_CONTACT;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.ANTI_HORM_VALUE;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.DEPA_RULE_NEW_INTERVENTION;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.INDIVIDUAL_VALUE;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.LOCK_SMITH_VALUE;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.customerType;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.getDistNewIntAndOldCustomer;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.getRealValue;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.infestationType;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.interventionType;
import static app.bpartners.api.repository.expressif.utils.ProspectEvalUtils.professionalCustomerType;
import static app.bpartners.api.service.ProspectService.defaultStatusHistoryEntity;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.ContactNature;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.ProspectFeedback;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.prospect.Prospect;
import app.bpartners.api.model.prospect.ProspectStatusHistory;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.repository.jpa.model.HProspectStatusHistory;
import app.bpartners.api.service.utils.CustomDateFormatter;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class ProspectMapper {
  public static final String PROSPECT_CONTACT_NATURE = "prospect";
  public static final String OLD_CUSTOMER_CONTACT_NATURE = "ancien client";
  public static final int OWNER_ID_CELL_INDEX = 31;
  private final BanApi banApi;
  private final CustomDateFormatter customDateFormatter;

  private static String checkIfOldOrNew(String toCheck, String expected) {
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
    return toEntity(domain, domain.getIdHolderOwner(), rating, lastEvaluationDate, existing);
  }

  // TODO: put this constraint check inside service not here
  public HProspect toEntity(
      Prospect domain,
      String prospectOwnerId,
      Double rating,
      Instant lastEvaluationDate,
      HProspect existingEntity) {
    Geojson location = domain.getLocation();
    HProspect entity =
        existingEntity == null
            ? HProspect.builder()
                .id(String.valueOf(randomUUID()))
                .idAccountHolder(prospectOwnerId)
                .statusHistories(defaultStatusHistoryEntity())
                .build()
            : existingEntity;
    List<HProspectStatusHistory> actualHistory = entity.getStatusHistories();

    boolean prospectIsResetToContact =
        existingEntity != null
            && existingEntity.getActualStatus() != TO_CONTACT
            && domain.getActualStatus().equals(TO_CONTACT);

    boolean prospectIsDeclined =
        domain.getProspectFeedback() != null
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
      List<HProspectStatusHistory> newStatusHistory =
          List.of(
              HProspectStatusHistory.builder()
                  .id(String.valueOf(randomUUID()))
                  .status(domain.getActualStatus())
                  .updatedAt(Instant.now())
                  .build());
      return entity.toBuilder()
          .id(domain.getId())
          .firstName(domain.getFirstName())
          .idJob(domain.getIdJob())
          .managerName(domain.getManagerName())
          .oldPhone(existingEntity == null ? domain.getPhone() : existingEntity.getOldPhone())
          .oldName(existingEntity == null ? domain.getName() : existingEntity.getOldName())
          .oldAddress(existingEntity == null ? domain.getAddress() : existingEntity.getOldAddress())
          .oldEmail(existingEntity == null ? domain.getEmail() : existingEntity.getOldEmail())
          .newPhone(
              existingEntity == null
                  ? null
                  : checkIfOldOrNew(domain.getPhone(), entity.getOldPhone()))
          .newName(
              existingEntity == null
                  ? null
                  : checkIfOldOrNew(domain.getName(), entity.getOldName()))
          .newAddress(
              existingEntity == null
                  ? null
                  : checkIfOldOrNew(domain.getAddress(), entity.getOldAddress()))
          .newEmail(
              existingEntity == null
                  ? null
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
          .contactNature(
              existingEntity == null
                  ? (domain.getContactNature() == null
                      ? ContactNature.PROSPECT
                      : domain.getContactNature())
                  : existingEntity.getContactNature())
          .contractAmount(
              domain.getContractAmount() == null ? null : domain.getContractAmount().toString())
          .build();
    }
  }

  public HProspect toEntity(
      Prospect domain, String prospectOwnerId, Double rating, Instant lastEvaluationDate) {
    List<HProspectStatusHistory> newStatusHistory =
        List.of(
            HProspectStatusHistory.builder()
                .id(String.valueOf(randomUUID()))
                .status(domain.getActualStatus())
                .updatedAt(Instant.now())
                .build());
    Geojson location = domain.getLocation();
    return HProspect.builder()
        .id(domain.getId())
        .firstName(domain.getFirstName())
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
        .firstName(entity.getFirstName())
        .idJob(entity.getIdJob())
        .idHolderOwner(entity.getIdAccountHolder())
        .email(isNewExists(entity.getNewEmail(), entity.getOldEmail()))
        .address(isNewExists(entity.getNewAddress(), entity.getOldAddress()))
        .name(isNewExists(entity.getNewName(), entity.getOldName()))
        .managerName(entity.getManagerName())
        .phone(isNewExists(entity.getNewPhone(), entity.getOldPhone()))
        .location(location)
        .statusHistories(
            entity.getStatusHistories().stream().map(this::toDomain).collect(Collectors.toList()))
        .townCode(entity.getTownCode())
        .rating(
            Prospect.ProspectRating.builder()
                .value(entity.getRating() == null ? -1 : entity.getRating())
                .lastEvaluationDate(entity.getLastEvaluationDate())
                .build())
        .comment(entity.getComment())
        .defaultComment(entity.getDefaultComment())
        .prospectFeedback(entity.getProspectFeedback())
        .idInvoice(entity.getIdInvoice())
        .contractAmount(
            entity.getContractAmount() == null ? null : parseFraction(entity.getContractAmount()))
        .contactNature(
            entity.getContactNature() == null ? ContactNature.PROSPECT : entity.getContactNature())
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

  public List<ProspectEval> toProspectEval(String ownerId, Sheet sheet) {
    var gridData = sheet.getData();
    List<ProspectEval> prospectList = new ArrayList<>();
    gridData.forEach(
        grid -> {
          int firstIndex = grid.getStartColumn() == null ? 0 : grid.getStartColumn();
          var rows = grid.getRowData();
          rows.forEach(
              rowData -> {
                if (!hasNullCellData(rowData)) {
                  var cells = rowData.getValues();
                  ProspectEval.Builder<Object> builder = ProspectEval.builder();
                  ProspectEvalInfo info =
                      toProspectEvalInfo(firstIndex, rowData).toBuilder().owner(ownerId).build();
                  builder.id(String.valueOf(randomUUID()));
                  builder.prospectEvalInfo(info);
                  builder.prospectOwnerId(ownerId);
                  setBuilderJobValue(builder, cells.get(15));
                  builder.insectControl(rowBooleanValue(cells.get(16)));
                  builder.disinfection(rowBooleanValue(cells.get(17)));
                  builder.ratRemoval(rowBooleanValue(cells.get(18)));
                  builder.particularCustomer(rowBooleanValue(cells.get(19)));
                  builder.professionalCustomer(rowBooleanValue(cells.get(20)));

                  String depaRuleValue = cells.get(14).getFormattedValue();
                  if (depaRuleValue.equals(DEPA_RULE_NEW_INTERVENTION)) {
                    String newIntAddress = cells.get(24).getFormattedValue();
                    GeoPosition newIntPos = banApi.fSearch(newIntAddress);
                    String oldCustomerAddress = cells.get(27).getFormattedValue();
                    builder.depaRule(
                        NewIntervention.builder()
                            .planned(rowBooleanValue(cells.get(21)))
                            .interventionType(getInterventionType(cells.get(22)))
                            .infestationType(getInfestationType(cells.get(23)))
                            .newIntAddress(newIntAddress)
                            .distNewIntAndProspect(
                                newIntPos == null || info.getCoordinates() == null
                                    ? null
                                    : info.getCoordinates()
                                        .getDistanceFrom(newIntPos.getCoordinates()))
                            .coordinate(newIntPos == null ? null : newIntPos.getCoordinates())
                            .oldCustomer(
                                NewIntervention.OldCustomer.builder()
                                    .idCustomer(
                                        null) // Here to make it more explicit, we show actual
                                    // customer value
                                    .type(getCustomerType(cells.get(25)))
                                    .professionalType(getProfessionalType(cells.get(26)))
                                    // TODO: Must be provided from database customer
                                    .oldCustomerAddress(oldCustomerAddress)
                                    // Explicitly, this distance is provided from provided address
                                    .distNewIntAndOldCustomer(
                                        oldCustomerAddress == null
                                            ? null
                                            : getDistNewIntAndOldCustomer(
                                                banApi, newIntPos, oldCustomerAddress))
                                    .build())
                            .build());
                  } else {
                    throw new NotImplementedException(
                        "Only " + DEPA_RULE_NEW_INTERVENTION + " is supported for now");
                  }
                  prospectList.add(builder.build());
                }
              });
        });
    return prospectList;
  }

  public List<ProspectEval> toProspectEval(Sheet sheet) {
    var gridData = sheet.getData();
    List<ProspectEval> prospectList = new ArrayList<>();
    gridData.forEach(
        grid -> {
          int firstIndex = grid.getStartColumn() == null ? 0 : grid.getStartColumn();
          var rows = grid.getRowData();
          rows.forEach(
              rowData -> {
                if (!hasNullCellData(rowData)) {
                  var cells = rowData.getValues();
                  ProspectEval.Builder<Object> builder = ProspectEval.builder();
                  ProspectEvalInfo info = toProspectEvalInfo(firstIndex, rowData);
                  builder.id(String.valueOf(randomUUID()));
                  builder.prospectEvalInfo(info);
                  builder.prospectOwnerId(info.getOwner());
                  setBuilderJobValue(builder, cells.get(15));
                  builder.insectControl(rowBooleanValue(cells.get(16)));
                  builder.disinfection(rowBooleanValue(cells.get(17)));
                  builder.ratRemoval(rowBooleanValue(cells.get(18)));
                  builder.particularCustomer(rowBooleanValue(cells.get(19)));
                  builder.professionalCustomer(rowBooleanValue(cells.get(20)));

                  String depaRuleValue = cells.get(14).getFormattedValue();
                  if (depaRuleValue.equals(DEPA_RULE_NEW_INTERVENTION)) {
                    String newIntAddress = cells.get(24).getFormattedValue();
                    GeoPosition newIntPos = banApi.fSearch(newIntAddress);
                    String oldCustomerAddress = cells.get(27).getFormattedValue();
                    builder.depaRule(
                        NewIntervention.builder()
                            .planned(rowBooleanValue(cells.get(21)))
                            .interventionType(getInterventionType(cells.get(22)))
                            .infestationType(getInfestationType(cells.get(23)))
                            .newIntAddress(newIntAddress)
                            .distNewIntAndProspect(
                                newIntPos == null || info.getCoordinates() == null
                                    ? null
                                    : info.getCoordinates()
                                        .getDistanceFrom(newIntPos.getCoordinates()))
                            .coordinate(newIntPos == null ? null : newIntPos.getCoordinates())
                            .oldCustomer(
                                NewIntervention.OldCustomer.builder()
                                    .idCustomer(
                                        null) // Here to make it more explicit, we show actual
                                    // customer value
                                    .type(getCustomerType(cells.get(25)))
                                    .professionalType(getProfessionalType(cells.get(26)))
                                    // TODO: Must be provided from database customer
                                    .oldCustomerAddress(oldCustomerAddress)
                                    // Explicitly, this distance is provided from provided address
                                    .distNewIntAndOldCustomer(
                                        oldCustomerAddress == null
                                            ? null
                                            : getDistNewIntAndOldCustomer(
                                                banApi, newIntPos, oldCustomerAddress))
                                    .build())
                            .build());
                  } else {
                    throw new NotImplementedException(
                        "Only " + DEPA_RULE_NEW_INTERVENTION + " is supported for now");
                  }
                  prospectList.add(builder.build());
                }
              });
        });
    return prospectList;
  }

  private void setBuilderJobValue(ProspectEval.Builder<Object> builder, CellData cellData) {
    String jobValue = cellData.getFormattedValue();
    if (jobValue.equals(ANTI_HORM_VALUE)) {
      builder.antiHarm(true);
      builder.lockSmith(false);
    } else if (jobValue.equals(LOCK_SMITH_VALUE)) {
      builder.antiHarm(false);
      builder.lockSmith(true);
    } else {
      throw new NotImplementedException(
          "Only \""
              + ANTI_HORM_VALUE
              + "\" or \""
              + LOCK_SMITH_VALUE
              + "\" is supported for now. Otherwise, "
              + jobValue
              + " was given");
    }
  }

  public ProspectEvalInfo toProspectEvalInfo(int firstIndex, RowData rowData) {
    List<ProspectInfoPropertyAction> prospectPropertyActions = getInfoPropertyActions();
    var infoBuilder = ProspectEvalInfo.builder();
    var cells = rowData.getValues();
    for (int index = firstIndex;
        index < cells.size() && index < firstIndex + prospectPropertyActions.size();
        index++) {
      int builderIndex = index - firstIndex;
      CellData currentCell;
      if (builderIndex != prospectPropertyActions.size() - 1) {
        currentCell = cells.get(index);
      } else {
        currentCell = cells.get(OWNER_ID_CELL_INDEX);
      }
      ProspectInfoPropertyAction action = prospectPropertyActions.get(builderIndex);
      action.performAction(infoBuilder, currentCell);
    }
    ProspectEvalInfo info = infoBuilder.build();
    if (info.getAddress() == null) {
      throw new BadRequestException("Address is missing for Prospect(name=" + info.getName() + ")");
    } else {
      GeoPosition geoPosition = banApi.fSearch(info.getAddress());
      info.setCoordinates(geoPosition == null ? null : geoPosition.getCoordinates());
    }
    return info;
  }

  public List<ProspectEvalInfo> toProspectEvalInfo(Sheet sheet) {
    var gridData = sheet.getData();
    List<ProspectEvalInfo> prospectList = new ArrayList<>();
    List<ProspectInfoPropertyAction> prospectPropertyActions = getInfoPropertyActions();
    gridData.forEach(
        grid -> {
          int firstIndex = grid.getStartColumn() == null ? 0 : grid.getStartColumn();
          var row = grid.getRowData();
          row.forEach(
              rowData -> {
                if (!hasNullCellData(rowData)) {
                  var prospectBuilder = ProspectEvalInfo.builder();
                  var cells = rowData.getValues();
                  for (int index = firstIndex;
                      index < cells.size() && index < firstIndex + prospectPropertyActions.size();
                      index++) {
                    int builderIndex = index - firstIndex;
                    CellData currentCell;
                    if (builderIndex != prospectPropertyActions.size() - 1) {
                      currentCell = cells.get(index);
                    } else {
                      currentCell = cells.get(OWNER_ID_CELL_INDEX);
                    }
                    ProspectInfoPropertyAction action = prospectPropertyActions.get(builderIndex);
                    action.performAction(prospectBuilder, currentCell);
                  }
                  prospectList.add(prospectBuilder.build());
                }
              });
        });
    return prospectList;
  }

  public static ProspectEvalInfo.ContactNature getContactNature(String value) {
    String formattedValue = value.trim().toLowerCase();
    switch (formattedValue) {
      case PROSPECT_CONTACT_NATURE:
        return ProspectEvalInfo.ContactNature.PROSPECT;
      case OLD_CUSTOMER_CONTACT_NATURE:
        return ProspectEvalInfo.ContactNature.OLD_CUSTOMER;
      case "null":
        return null;
      default:
        log.warn("Unknown contact nature value " + value);
        return ProspectEvalInfo.ContactNature.OTHER;
    }
  }

  private String getInterventionType(CellData cellData) {
    String stringValue = cellData.getFormattedValue();
    String realValue = getRealValue(stringValue);
    if (realValue != null && !Arrays.asList(interventionType()).contains(realValue)) {
      throw new BadRequestException("Bad intervention type : " + stringValue);
    }
    return realValue;
  }

  private String getInfestationType(CellData cellData) {
    String stringValue = cellData.getFormattedValue();
    String realValue = getRealValue(stringValue);
    if (!Arrays.asList(infestationType()).contains(realValue)) {
      throw new BadRequestException("Bad infestation type : " + stringValue);
    }
    return realValue;
  }

  private Boolean rowBooleanValue(CellData cellData) {
    String stringValue = cellData.getFormattedValue();
    Boolean bool =
        stringValue == null
            ? null
            : (stringValue.equals("Yes")
                ? Boolean.TRUE
                : (stringValue.equals("No") ? Boolean.FALSE : null));
    return stringValue == null ? null : bool;
  }

  private NewIntervention.OldCustomer.OldCustomerType getCustomerType(CellData cellData) {
    String stringValue = cellData.getFormattedValue();
    String realValue = getRealValue(stringValue);
    if (realValue != null && !Arrays.asList(customerType()).contains(realValue)) {
      throw new BadRequestException("Bad customer type : " + stringValue);
    }
    return stringValue == null
        ? null
        : stringValue.equals(INDIVIDUAL_VALUE)
            ? NewIntervention.OldCustomer.OldCustomerType.INDIVIDUAL
            : NewIntervention.OldCustomer.OldCustomerType.PROFESSIONAL;
  }

  public String getProfessionalType(CellData cellData) {
    String stringValue = cellData.getFormattedValue();
    String realValue = getRealValue(stringValue);
    if (realValue != null && !Arrays.asList(professionalCustomerType()).contains(realValue)) {
      throw new BadRequestException("Bad professional type : " + stringValue);
    }
    return realValue;
  }

  private static boolean hasNullCellData(RowData rowData) {
    return rowData.getValues().stream().allMatch(cellData -> cellData.getFormattedValue() == null);
  }

  private List<ProspectInfoPropertyAction> getInfoPropertyActions() {
    List<ProspectInfoPropertyAction> prospectPropertyActions = new ArrayList<>();
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.name(currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.website(currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.category(currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.subcategory(currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.address(currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.phoneNumber(currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.email(currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.managerName(currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.mailSent(currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.postalCode(currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.city(currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) ->
            builder.companyCreationDate(
                customDateFormatter.from_dd_MM_YYYY(currentCell.getFormattedValue())));
    prospectPropertyActions.add(
        (builder, currentCell) ->
            builder.contactNature(getContactNature(currentCell.getFormattedValue())));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.defaultComment(currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.owner(currentCell.getFormattedValue()));
    return prospectPropertyActions;
  }

  interface ProspectInfoPropertyAction {
    void performAction(ProspectEvalInfo.Builder builder, CellData currentCell);
  }
}
