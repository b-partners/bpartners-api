package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobStatus;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobType;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.prospect.job.AntiHarmRules;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJob;
import app.bpartners.api.model.prospect.job.ProspectEvaluationJobRunner;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.expressif.ProspectEvaluation;
import app.bpartners.api.repository.expressif.ProspectEvaluationInfo;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.service.CustomerService;
import app.bpartners.api.service.utils.DateUtils;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.model.JobStatusValue.NOT_STARTED;
import static app.bpartners.api.repository.expressif.fact.NewIntervention.OldCustomer.OldCustomerType.INDIVIDUAL;
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
import static java.util.UUID.randomUUID;

@Component
@AllArgsConstructor
@Slf4j
public class ProspectEvaluationMapper {
  public static final int OWNER_ID_CELL_INDEX = 31;
  public static final int MAX_DISTANCE_LIMIT = 1_000;
  public static final String PROSPECT_CONTACT_NATURE = "prospect";
  public static final String OLD_CUSTOMER_CONTACT_NATURE = "ancien client";

  public ProspectEvaluationJob toEvaluationJob(String ahId, ProspectEvaluationJobRunner jobRunner) {
    return ProspectEvaluationJob.builder()
        .id(jobRunner.getJobId())
        .metadata(jobRunner.getMetadata())
        .idAccountHolder(ahId)
        .type(getJobType(jobRunner))
        .jobStatus(new ProspectEvaluationJobStatus()
            .value(NOT_STARTED)
            .message(null))
        .startedAt(Instant.now())
        .endedAt(null)
        .results(List.of())
        .build();
  }

  public List<ProspectEvaluation> toProspectEvaluation(Sheet sheet, BanApi banApi) {
    var gridData = sheet.getData();
    List<ProspectEvaluation> prospectList = new ArrayList<>();
    gridData.forEach(grid -> {
      int firstIndex = grid.getStartColumn() == null ? 0 : grid.getStartColumn();
      var rows = grid.getRowData();
      rows.forEach(rowData -> {
        if (!hasNullCellData(rowData)) {
          var cells = rowData.getValues();

          ProspectEvaluation.Builder<Object> builder = ProspectEvaluation.builder();
          ProspectEvaluationInfo evaluationInfo = toProspectEvaluationInfo(firstIndex, rowData, banApi);
          builder.id(String.valueOf(randomUUID()));
          builder.prospectEvaluationInfo(evaluationInfo);
          builder.prospectOwnerId(evaluationInfo.getOwner());
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
            builder.depaRule(NewIntervention.builder()
                .planned(rowBooleanValue(cells.get(21)))
                .interventionType(getInterventionType(cells.get(22)))
                .infestationType(getInfestationType(cells.get(23)))
                .newIntAddress(newIntAddress)
                .distNewIntAndProspect(
                    newIntPos == null || evaluationInfo.getCoordinates() == null ? null
                        : evaluationInfo.getCoordinates()
                        .getDistanceFrom(newIntPos.getCoordinates()))
                .coordinate(newIntPos == null ? null : newIntPos.getCoordinates())
                .oldCustomer(NewIntervention.OldCustomer.builder()
                    .idCustomer(null) //Here to make it more explicit, we show actual customer value
                    .type(getCustomerType(cells.get(25)))
                    .professionalType(getProfessionalType(cells.get(26)))
                    //TODO: Must be provided from database customer
                    .oldCustomerAddress(oldCustomerAddress)
                    //Explicitly, this distance is provided from provided address
                    .distNewIntAndOldCustomer(oldCustomerAddress == null ? null
                        : getDistNewIntAndOldCustomer(banApi, newIntPos, oldCustomerAddress))
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

  private boolean hasNullCellData(RowData rowData) {
    return rowData.getValues().stream()
        .allMatch(cellData -> cellData.getFormattedValue() == null);
  }

  public ProspectEvaluationInfo.ContactNature getContactNature(String value) {
    String formattedValue = value.trim().toLowerCase();
    switch (formattedValue) {
      case PROSPECT_CONTACT_NATURE:
        return ProspectEvaluationInfo.ContactNature.PROSPECT;
      case OLD_CUSTOMER_CONTACT_NATURE:
        return ProspectEvaluationInfo.ContactNature.OLD_CUSTOMER;
      case "null":
        return null;
      default:
        log.warn("Unknown contact nature value " + value);
        return ProspectEvaluationInfo.ContactNature.OTHER;
    }
  }

  private void setBuilderJobValue(ProspectEvaluation.Builder<Object> builder,
                                  CellData cellData) {
    String jobValue = cellData.getFormattedValue();
    if (jobValue.equals(ANTI_HORM_VALUE)) {
      builder.antiHarm(true);
      builder.lockSmith(false);
    } else if (jobValue.equals(LOCK_SMITH_VALUE)) {
      builder.antiHarm(false);
      builder.lockSmith(true);
    } else {
      throw new NotImplementedException(
          "Only \"" + ANTI_HORM_VALUE + "\" or \"" + LOCK_SMITH_VALUE
              + "\" is supported for now. Otherwise, " + jobValue + " was given");
    }
  }

  public ProspectEvaluationInfo toProspectEvaluationInfo(int firstIndex,
                                                         RowData rowData,
                                                         BanApi banApi) {
    List<ProspectInfoPropertyAction> prospectPropertyActions =
        getInfoPropertyActions();
    var infoBuilder = ProspectEvaluationInfo.builder();
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
    ProspectEvaluationInfo info = infoBuilder.build();
    if (info.getAddress() == null) {
      throw new BadRequestException("Address is missing for Prospect(name=" + info.getName() + ")");
    } else {
      GeoPosition geoPosition = banApi.fSearch(info.getAddress());
      info.setCoordinates(
          geoPosition == null ? null : geoPosition.getCoordinates());
    }
    return info;
  }

  public List<ProspectEvaluationInfo> toProspectEvaluationInfo(Sheet sheet) {
    var gridData = sheet.getData();
    List<ProspectEvaluationInfo> prospectList = new ArrayList<>();
    List<ProspectInfoPropertyAction> prospectPropertyActions =
        getInfoPropertyActions();
    gridData.forEach(grid -> {
      int firstIndex = grid.getStartColumn() == null ? 0 : grid.getStartColumn();
      var row = grid.getRowData();
      row.forEach(rowData -> {
        if (!hasNullCellData(rowData)) {
          var prospectBuilder = ProspectEvaluationInfo.builder();
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
            ProspectInfoPropertyAction action =
                prospectPropertyActions.get(builderIndex);
            action.performAction(prospectBuilder, currentCell);
          }
          prospectList.add(prospectBuilder.build());
        }
      });
    });
    return prospectList;
  }

  public List<ProspectEvaluation> toCustomerProspectEvaluation(String ahId,
                                                               AntiHarmRules antiHarmRules,
                                                               List<ProspectEvaluation> prospectEvaluations,
                                                               CustomerService customerService) {
    HashMap<String, List<ProspectEvaluation>> groupByOwner =
        prospectEvaluations.isEmpty() ? ownerHashMap(ahId)
            : dispatchEvaluationByAccountHolder(prospectEvaluations);
    List<ProspectEvaluation> customersToEvaluate = new ArrayList<>();

    for (Map.Entry<String, List<ProspectEvaluation>> entry : groupByOwner.entrySet()) {
      String accountHolderId = entry.getKey();
      List<ProspectEvaluation> subList = entry.getValue();
      List<Customer> customers = customerService.findByAccountHolderId(accountHolderId);

      for (ProspectEvaluation newProspectEvaluation : subList) {
        NewIntervention newIntervention = (NewIntervention) newProspectEvaluation.getDepaRule();

        for (Customer customer : customers) {
          if (customer.getLocation() == null || customer.getLocation().getCoordinate() == null
              || customer.getLocation().getCoordinate().getLatitude() == null
              || customer.getLocation().getCoordinate().getLongitude() == null) {
            continue;
          }
          Double distance = newIntervention.getCoordinate()
              .getDistanceFrom(customer.getLocation().getCoordinate());
          if (distance < MAX_DISTANCE_LIMIT) {
            NewIntervention.OldCustomer customerBuilder =
                newIntervention.getOldCustomer().toBuilder()
                    .idCustomer(customer.getId())
                    .oldCustomerAddress(customer.getAddress())
                    .distNewIntAndOldCustomer(distance)
                    .build();
            ProspectEvaluation.Builder prospectBuilder = newProspectEvaluation.toBuilder()
                .id(String.valueOf(randomUUID())) //new ID
                .depaRule(newIntervention.toBuilder()
                    .oldCustomer(customerBuilder)
                    .build());
            if (prospectEvaluations.isEmpty() && antiHarmRules != null) {
              prospectBuilder.particularCustomer(true);
              prospectBuilder.professionalCustomer(false);
              prospectBuilder.insectControl(antiHarmRules.isInsectControl());
              prospectBuilder.disinfection(antiHarmRules.isDisinfection());
              prospectBuilder.ratRemoval(antiHarmRules.isRatRemoval());
              prospectBuilder.depaRule(newIntervention.toBuilder()
                  .oldCustomer(customerBuilder.toBuilder()
                      .type(INDIVIDUAL)
                      .build())
                  .build());
            }
            /* /!\ Because here we only evaluate then save in another step
             * Conversion of customers to new prospect is done here*/
            prospectBuilder.prospectEvaluationInfo(ProspectEvaluationInfo.builder()
                .owner(accountHolderId)
                .name(customer.getName())
                .managerName(customer.getName())
                .email(customer.getEmail())
                .phoneNumber(customer.getPhone())
                .address(customer.getAddress())
                .city(customer.getCity())
                .coordinates(customer.getLocation().getCoordinate())
                .postalCode(String.valueOf(customer.getZipCode()))
                .contactNature(ProspectEvaluationInfo.ContactNature.OLD_CUSTOMER)
                .category("Restaurant") //TODO: deprecated, but for now we will set it by default
                .subcategory("Restaurant") //TODO: deprecated, but for now we will set it by default
                .build());
            customersToEvaluate.add(prospectBuilder.build());
          }
        }
      }
    }
    return customersToEvaluate;
  }


  private ProspectEvaluationJobType getJobType(ProspectEvaluationJobRunner job) {
    if (job.isEventConversionJob()) {
      return ProspectEvaluationJobType.CALENDAR_EVENT_CONVERSION;
    } else if (job.isSpreadsheetEvaluationJob()) {
      return ProspectEvaluationJobType.SPREADSHEET_EVALUATION;
    }
    throw new NotImplementedException(
        "Only prospect evaluation job type [CALENDAR_EVENT_CONVERSION and SPREADSHEET_EVALUATION] are supported for now");
  }


  private HashMap<String, List<ProspectEvaluation>> ownerHashMap(String ahId) {
    HashMap<String, List<ProspectEvaluation>> ownerHasMap = new HashMap<>();
    ownerHasMap.put(ahId, new ArrayList<>());
    return ownerHasMap;
  }

  private HashMap<String, List<ProspectEvaluation>> dispatchEvaluationByAccountHolder(
      List<ProspectEvaluation> prospectEvaluations) {
    HashMap<String, List<ProspectEvaluation>> prospectMap = new HashMap<>();
    for (ProspectEvaluation prospectEvaluation : prospectEvaluations) {
      if (prospectEvaluation.isNewIntervention()) {
        String accountHolderId = prospectEvaluation.getProspectOwnerId();
        if (!prospectMap.containsKey(accountHolderId)) {
          List<ProspectEvaluation> subList = new ArrayList<>();
          subList.add(prospectEvaluation);
          prospectMap.put(accountHolderId, subList);
        } else {
          prospectMap.get(accountHolderId).add(prospectEvaluation);
        }
      }
    }
    return prospectMap;
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
    Boolean bool = stringValue == null ? null : (stringValue.equals("Yes")
        ? Boolean.TRUE : (stringValue.equals("No") ? Boolean.FALSE : null));
    return stringValue == null ? null : bool;
  }

  private NewIntervention.OldCustomer.OldCustomerType getCustomerType(CellData cellData) {
    String stringValue = cellData.getFormattedValue();
    String realValue = getRealValue(stringValue);
    if (realValue != null && !Arrays.asList(customerType()).contains(realValue)) {
      throw new BadRequestException("Bad customer type : " + stringValue);
    }
    return stringValue == null ? null
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
    prospectPropertyActions.add((builder, currentCell) -> builder.companyCreationDate(
        DateUtils.from_dd_MM_YYYY(currentCell.getFormattedValue())));
    prospectPropertyActions.add((builder, currentCell) -> builder.contactNature(
        getContactNature(currentCell.getFormattedValue())));
    prospectPropertyActions.add((builder, currentCell) -> builder.defaultComment(
        currentCell.getFormattedValue()));
    prospectPropertyActions.add(
        (builder, currentCell) -> builder.owner(currentCell.getFormattedValue()));
    return prospectPropertyActions;
  }

  interface ProspectInfoPropertyAction {
    void performAction(ProspectEvaluationInfo.Builder builder, CellData currentCell);
  }
}
