package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.ProspectFeedback;
import app.bpartners.api.endpoint.rest.model.ProspectStatus;
import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import app.bpartners.api.service.utils.DateUtils;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.RowData;
import com.google.api.services.sheets.v4.model.Sheet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
@Slf4j
public class ProspectMapper {

  public static final String PROSPECT_CONTACT_NATURE = "prospect";
  public static final String OLD_CUSTOMER_CONTACT_NATURE = "ancien client";
  public static final int OWNER_ID_CELL_INDEX = 30;
  private final AuthenticatedResourceProvider provider;
  private final ProspectJpaRepository jpaRepository;

  private static String checkIfOldOrNew(String toCheck,
                                        String expected) {
    if (toCheck != null && expected != null) {
      return !expected.equals(toCheck) ? toCheck : null;
    } else if (expected == null && toCheck != null) {
      return toCheck;
    }
    return expected;
  }

  private static String isNewExists(String toCheck, String actual) {
    return toCheck != null ? toCheck : actual;
  }

  public HProspect toEntity(Prospect domain) {
    Optional<HProspect> optionalProspect = jpaRepository.findById(domain.getId());
    if (optionalProspect.isEmpty()) {
      throw new NotFoundException("Prospect." + domain.getId() + " not found. ");
    }
    HProspect existing = optionalProspect.get();
    return toEntity(domain,
        provider.getDefaultAccountHolder().getId(),
        existing.getRating(),
        existing.getLastEvaluationDate(), existing);
  }

  public HProspect toEntity(Prospect domain, String prospectOwnerId, Double rating,
                            Instant lastEvaluationDate, HProspect actualEntity) {
    Geojson location = domain.getLocation();
    if (domain.getStatus().equals(ProspectStatus.TO_CONTACT)
        || (domain.getProspectFeedback() != null
        && domain.getProspectFeedback().equals(ProspectFeedback.NOT_INTERESTED))) {
      return actualEntity.toBuilder()
          .newName(null)
          .newEmail(null)
          .newPhone(null)
          .newAddress(null)
          .idAccountHolder(null)
          .comment(null)
          .contractAmount(null)
          .prospectFeedback(null)
          .idInvoice(null)
          .status(ProspectStatus.TO_CONTACT)
          .build();
    } else {
      return actualEntity.toBuilder()
          .id(domain.getId())
          .newPhone(checkIfOldOrNew(domain.getPhone(), actualEntity.getOldPhone()))
          .newName(checkIfOldOrNew(domain.getName(), actualEntity.getOldName()))
          .newAddress(checkIfOldOrNew(domain.getAddress(), actualEntity.getOldAddress()))
          .newEmail(checkIfOldOrNew(domain.getEmail(), actualEntity.getOldEmail()))
          .status(domain.getStatus())
          .idAccountHolder(prospectOwnerId)
          .townCode(domain.getTownCode())
          .rating(rating)
          .lastEvaluationDate(lastEvaluationDate)
          .posLongitude(location == null ? null : location.getLongitude())
          .posLatitude(location == null ? null : location.getLatitude())
          .comment(domain.getComment())
          .idInvoice(domain.getIdInvoice())
          .prospectFeedback(domain.getProspectFeedback())
          .contractAmount(
              domain.getContractAmount() == null ? null : domain.getContractAmount().toString())
          .build();
    }
  }

  public HProspect toEntity(Prospect domain, String prospectOwnerId, Double rating,
                            Instant lastEvaluationDate) {
    Geojson location = domain.getLocation();
    return HProspect.builder()
        .id(domain.getId())
        .oldPhone(domain.getPhone())
        .oldName(domain.getName())
        .oldEmail(domain.getEmail())
        .status(domain.getStatus())
        .oldAddress(domain.getAddress())
        .idAccountHolder(prospectOwnerId)
        .townCode(domain.getTownCode())
        .rating(rating)
        .lastEvaluationDate(lastEvaluationDate)
        .posLongitude(location == null ? null : location.getLongitude())
        .posLatitude(location == null ? null : location.getLatitude())
        .build();
  }

  public Prospect toDomain(HProspect entity, Geojson location) {
    return Prospect.builder()
        .id(entity.getId())
        .idHolderOwner(entity.getIdAccountHolder())
        .email(isNewExists(entity.getNewEmail(), entity.getOldEmail()))
        .address(isNewExists(entity.getNewAddress(), entity.getOldAddress()))
        .name(isNewExists(entity.getNewName(), entity.getOldName()))
        .phone(isNewExists(entity.getNewPhone(), entity.getOldPhone()))
        .location(location)
        .status(entity.getStatus())
        .townCode(entity.getTownCode())
        .rating(Prospect.ProspectRating.builder()
            .value(entity.getRating())
            .lastEvaluationDate(entity.getLastEvaluationDate())
            .build())
        .comment(entity.getComment())
        .prospectFeedback(entity.getProspectFeedback())
        .idInvoice(entity.getIdInvoice())
        .contractAmount(entity.getContractAmount() == null
            ? null
            : parseFraction(entity.getContractAmount()))
        .build();
  }

  public List<ProspectEvalInfo> toProspect(Sheet sheet) {
    var gridData = sheet.getData();
    List<ProspectEvalInfo> prospectList = new ArrayList<>();
    List<PropertyAction> propertyActions = getPropertyActions();
    gridData.forEach(grid -> {
      int firstIndex = grid.getStartColumn() == null ? 0 : grid.getStartColumn();
      var row = grid.getRowData();
      row.forEach(rowData -> {
        if (!hasNullCellData(rowData)) {
          var prospectBuilder = ProspectEvalInfo.builder();
          var cells = rowData.getValues();
          for (int index = firstIndex;
               index < cells.size() && index < firstIndex + propertyActions.size();
               index++) {
            int builderIndex = index - firstIndex;
            CellData currentCell;
            if (builderIndex != propertyActions.size() - 1) {
              currentCell = cells.get(index);
            } else {
              currentCell = cells.get(OWNER_ID_CELL_INDEX);
            }
            PropertyAction action = propertyActions.get(builderIndex);
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

  private static boolean hasNullCellData(RowData rowData) {
    return rowData.getValues().stream()
        .allMatch(cellData -> cellData.getFormattedValue() == null);
  }

  private static List<PropertyAction> getPropertyActions() {
    List<PropertyAction> propertyActions = new ArrayList<>();
    propertyActions.add((builder, currentCell) -> builder.name(currentCell.getFormattedValue()));
    propertyActions.add((builder, currentCell) -> builder.website(currentCell.getFormattedValue()));
    propertyActions.add(
        (builder, currentCell) -> builder.category(currentCell.getFormattedValue()));
    propertyActions.add(
        (builder, currentCell) -> builder.subcategory(currentCell.getFormattedValue()));
    propertyActions.add((builder, currentCell) -> builder.address(currentCell.getFormattedValue()));
    propertyActions.add(
        (builder, currentCell) -> builder.phoneNumber(currentCell.getFormattedValue()));
    propertyActions.add((builder, currentCell) -> builder.email(currentCell.getFormattedValue()));
    propertyActions.add(
        (builder, currentCell) -> builder.managerName(currentCell.getFormattedValue()));
    propertyActions.add(
        (builder, currentCell) -> builder.mailSent(currentCell.getFormattedValue()));
    propertyActions.add(
        (builder, currentCell) -> builder.postalCode(currentCell.getFormattedValue()));
    propertyActions.add((builder, currentCell) -> builder.city(currentCell.getFormattedValue()));
    propertyActions.add((builder, currentCell) -> builder.companyCreationDate(
        DateUtils.from_dd_MM_YYYY(currentCell.getFormattedValue()))
    );
    propertyActions.add((builder, currentCell) -> builder.contactNature(
        getContactNature(currentCell.getFormattedValue())));
    propertyActions.add((builder, currentCell) -> builder.owner(currentCell.getFormattedValue()));
    return propertyActions;
  }

  interface PropertyAction {
    void performAction(ProspectEvalInfo.Builder builder, CellData currentCell);
  }
}
