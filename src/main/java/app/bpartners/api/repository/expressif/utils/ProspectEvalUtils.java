package app.bpartners.api.repository.expressif.utils;

import app.bpartners.api.endpoint.rest.model.EvaluatedProspect;
import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.InterventionResult;
import app.bpartners.api.endpoint.rest.model.OldCustomerResult;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.ban.BanApi;
import app.bpartners.api.repository.ban.model.GeoPosition;
import app.bpartners.api.repository.expressif.ProspectEval;
import app.bpartners.api.repository.expressif.ProspectEvalInfo;
import app.bpartners.api.repository.expressif.fact.NewIntervention;
import app.bpartners.api.repository.expressif.fact.Robbery;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.expressif.ProspectEvalInfo.ContactNature.OTHER;
import static app.bpartners.api.repository.expressif.ProspectEvalInfo.ContactNature.PROSPECT;
import static java.util.UUID.randomUUID;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_ERROR;

@Slf4j
@Component
@AllArgsConstructor
public class ProspectEvalUtils {
  public static final int FIRST_SHEET_INDEX = 0;
  public static final String PROSPECT_NATURE_VALUE = "Prospect";
  public static final int DEPA_RULE_COL_INDEX = 13;
  public static final String DEPA_RULE_NEW_INTERVENTION = "Dépa1 / Nouvelle intervention";
  public static final String DEPA_RULE_ROBBERY = "Dépa1 / Cambriolage";
  public static final int JOB_CELL_INDEX = 14;
  public static final String ANTI_HORM_VALUE = "Antinuisibles 3D";
  public static final String LOCK_SMITH_VALUE = "Serrurier";
  public static final String INDIVIDUAL_VALUE = "particulier";
  public static final int PROSPECT_ADDRESS_COL_INDEX = 4;
  public static final int NEW_INT_ADDRESS_COL_INDEX = 23;
  public static final int NEW_INT_OLD_CUST_ADDRESS_COL_INDEX = 26;
  public static final int ROB_OLD_CUST_ADDRESS_COL_INDEX = 29;
  public static final int ROBB_ADDRESS_COL_INDEX = 28;
  private final BanApi banApi;

  @SneakyThrows
  public static byte[] convertIntoExcel(
      ByteArrayInputStream inputProspectStream, List<EvaluatedProspect> results) {
    ByteArrayOutputStream outputStream;
    try (Workbook workbook = WorkbookFactory.create(inputProspectStream)) {
      Sheet sheet = workbook.createSheet("Résultats évaluation");

      Row headerRow = sheet.createRow(0);
      headerRow.createCell(0).setCellValue("ID");
      headerRow.createCell(1).setCellValue("Référence");
      headerRow.createCell(2).setCellValue("Nom");
      headerRow.createCell(3).setCellValue("Email");
      headerRow.createCell(4).setCellValue("Site web");
      headerRow.createCell(5).setCellValue("Téléphone");
      headerRow.createCell(6).setCellValue("Adresse");
      headerRow.createCell(7).setCellValue("Ville");
      headerRow.createCell(8).setCellValue("Nom du gérant");
      headerRow.createCell(9).setCellValue("Nature du contact");
      headerRow.createCell(10).setCellValue("Date d'évaluation");
      headerRow.createCell(11).setCellValue("Position - Latitude");
      headerRow.createCell(12).setCellValue("Position - Longitude");
      headerRow.createCell(13).setCellValue("Position - Carte");
      headerRow.createCell(14).setCellValue("Intervention - Notation sur 10");
      headerRow.createCell(15).setCellValue("Intervention - Adresse");
      headerRow.createCell(16).setCellValue("Intervention - Distance depuis le prospect");
      headerRow.createCell(17).setCellValue("Ancien client - Notation sur 10");
      headerRow.createCell(18).setCellValue("Ancien client - Adresse");
      headerRow.createCell(19).setCellValue("Ancien client - Distance depuis le prospect");

      int row = 1;
      for (EvaluatedProspect eval : results) {
        Row currentRow = sheet.createRow(row);
        currentRow.createCell(0).setCellValue(eval.getId());
        currentRow.createCell(1).setCellValue(eval.getReference());
        currentRow.createCell(2).setCellValue(eval.getName());
        currentRow.createCell(3).setCellValue(eval.getEmail());
        currentRow.createCell(4).setCellValue(eval.getWebsite());
        currentRow.createCell(5).setCellValue(eval.getPhone());
        currentRow.createCell(6).setCellValue(eval.getAddress());
        currentRow.createCell(7).setCellValue(eval.getCity());
        currentRow.createCell(8).setCellValue(eval.getManagerName());
        currentRow.createCell(9).setCellValue(eval.getContactNature().getValue());
        currentRow.createCell(10).setCellValue(eval.getEvaluationDate() == null ? null
            : Date.from(eval.getEvaluationDate()));

        Geojson geojson = eval.getArea() == null ? null
            : eval.getArea().getGeojson();
        currentRow.createCell(11).setCellValue(geojson == null ? null
            : geojson.getLatitude());
        currentRow.createCell(12).setCellValue(geojson == null ? null
            : geojson.getLongitude());
        currentRow.createCell(13).setCellValue(geojson == null ? null
            //TODO: make this customizable from other maps service
            : "https://www.latlong.net/c/?lat=" + geojson.getLatitude() + "&long="
            + geojson.getLongitude());

        InterventionResult interventionResult = eval.getInterventionResult();
        currentRow.createCell(14).setCellValue(interventionResult == null ? null
            : String.valueOf(interventionResult.getValue()));
        currentRow.createCell(15).setCellValue(interventionResult == null ? null
            : interventionResult.getAddress());
        currentRow.createCell(16).setCellValue(interventionResult == null ? null
            : String.valueOf(interventionResult.getDistanceFromProspect()));

        OldCustomerResult oldCustomerResult = eval.getOldCustomerResult();
        currentRow.createCell(17).setCellValue(
            String.valueOf(oldCustomerResult == null ? null
                : oldCustomerResult.getValue()));
        currentRow.createCell(18).setCellValue(oldCustomerResult == null ? null
            : oldCustomerResult.getAddress());
        currentRow.createCell(19).setCellValue(oldCustomerResult == null ? null
            : String.valueOf(oldCustomerResult.getDistanceFromProspect()));
        row++;
      }

      outputStream = new ByteArrayOutputStream();
      workbook.write(outputStream);
      workbook.close();
      outputStream.close();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
    return outputStream.toByteArray();
  }

  @SneakyThrows
  public List<ProspectEval> convertFromExcel(InputStream file) {
    Workbook workbook = WorkbookFactory.create(file);
    Sheet sheet = workbook.getSheetAt(FIRST_SHEET_INDEX);
    List<ProspectEval> prospectEvalList = new ArrayList<>();

    StringBuilder exceptionMsgBuilder = new StringBuilder();
    Iterator<Row> rows = sheet.rowIterator();
    while (rows.hasNext()) {
      Row currentRow = rows.next();
      if (currentRow.getRowNum() > 1) {
        String depaRuleValue = getStringValue(currentRow.getCell(DEPA_RULE_COL_INDEX));
        if (depaRuleValue == null) {
          addMissingException(exceptionMsgBuilder, currentRow);
        } else {
          ProspectEval prospectEval = new ProspectEval<>();
          ProspectEvalInfo prospectEvalInfo = getNewProspect(currentRow, exceptionMsgBuilder);

          /*
          /!\ Pay attention ! For now, we duplicate the prospect to evaluate for each evaluation
          To associate new evaluation to existing prospectEval, we must identify the unique key.
          Case 1 : we get the unique ID from the Excel file
          Case 2 : we identify a unique prospect from some attributes
          Like firstName + lastName + email + address

          In any case, we would take more time to process the prospect evaluation file.
           */
          prospectEval.setId(String.valueOf(randomUUID()));
          prospectEval.setProspectOwnerId(getStringValue(currentRow.getCell(30)));
          setProspectJobValue(exceptionMsgBuilder, currentRow, prospectEval);
          prospectEval.setProspectEvalInfo(prospectEvalInfo);

          prospectEval.setInsectControl(rowBooleanValue(currentRow, 15, exceptionMsgBuilder));
          prospectEval.setDisinfection(rowBooleanValue(currentRow, 16, exceptionMsgBuilder));
          prospectEval.setRatRemoval(rowBooleanValue(currentRow, 17, exceptionMsgBuilder));
          prospectEval.setParticularCustomer(rowBooleanValue(currentRow, 18, exceptionMsgBuilder));
          prospectEval.setProfessionalCustomer(
              rowBooleanValue(currentRow, 19, exceptionMsgBuilder));

          if (depaRuleValue.equals(DEPA_RULE_NEW_INTERVENTION)) {
            String newIntAddress = getStringValue(currentRow.getCell(NEW_INT_ADDRESS_COL_INDEX));
            GeoPosition newIntPos = banApi.fSearch(newIntAddress);
            String oldCustomerAddress =
                getStringValue(currentRow.getCell(NEW_INT_OLD_CUST_ADDRESS_COL_INDEX));
            prospectEval.setDepaRule(NewIntervention.builder()
                .planned(rowBooleanValue(currentRow, 20, exceptionMsgBuilder))
                .interventionType(getInterventionType(currentRow, exceptionMsgBuilder))
                .infestationType(getInfestationType(currentRow, exceptionMsgBuilder))
                .newIntAddress(newIntAddress)
                .distNewIntAndProspect(
                    newIntPos == null || prospectEvalInfo.getCoordinates() == null ? null
                        : prospectEvalInfo.getCoordinates()
                        .getDistanceFrom(newIntPos.getCoordinates()))
                .coordinate(newIntPos == null ? null : newIntPos.getCoordinates())
                .oldCustomer(NewIntervention.OldCustomer.builder()
                    .idCustomer(null) //Here to make it more explicit, we show actual customer value
                    .type(getCustomerType(currentRow, exceptionMsgBuilder))
                    .professionalType(getProfessionalType(currentRow, exceptionMsgBuilder))
                    //TODO: Must be provided from database customer
                    .oldCustomerAddress(oldCustomerAddress)
                    //Explicitly, this distance is provided from provided address
                    .distNewIntAndOldCustomer(oldCustomerAddress == null ? null
                        : getDistNewIntAndOldCustomer(newIntPos, oldCustomerAddress))
                    .build())
                .build());
          } else if (depaRuleValue.equals(DEPA_RULE_ROBBERY)) {
            String oldCustAddress =
                getStringValue(currentRow.getCell(ROB_OLD_CUST_ADDRESS_COL_INDEX));
            String robberyAddress = getStringValue(currentRow.getCell(ROBB_ADDRESS_COL_INDEX));
            GeoPosition robbAddressPos = banApi.fSearch(robberyAddress);

            prospectEval.setDepaRule(Robbery.builder()
                .declared(rowBooleanValue(currentRow, 27, exceptionMsgBuilder))
                .robberyAddress(robberyAddress)
                .distRobberyAndProspect(
                    prospectEvalInfo.getCoordinates()
                        .getDistanceFrom(robbAddressPos.getCoordinates()))
                .oldCustomer(oldCustAddress == null ? null
                    : Robbery.OldCustomer.builder()
                    .address(oldCustAddress)
                    .distRobberyAndOldCustomer(
                        getDistRobberyAndOldCustomer(robbAddressPos, oldCustAddress))
                    .build())
                .build());
          } else {
            throw new NotImplementedException(
                "Only \"" + DEPA_RULE_NEW_INTERVENTION + "\" or \"" + DEPA_RULE_ROBBERY
                    + "\" is supported for now. Otherwise, " + depaRuleValue + " was given");
          }
          prospectEvalList.add(prospectEval);
        }
      }
    }
    workbook.close();
    String exceptionMsg = exceptionMsgBuilder.toString();
    if (!exceptionMsg.isEmpty()) {
      throw new BadRequestException(exceptionMsg);
    }
    return prospectEvalList;
  }

  public String getProfessionalType(Row currentRow, StringBuilder exceptionMsgBuilder) {
    int cellIndex = 25;
    String stringValue = getStringValue(currentRow.getCell(cellIndex));
    String realValue = getRealValue(stringValue);
    if (realValue != null && !Arrays.asList(professionalCustomerType()).contains(realValue)) {
      exceptionMsgBuilder.append(position(currentRow, cellIndex))
          .append(" only support these values ")
          .append(Arrays.toString(professionalCustomerType()))
          .append(" but was ")
          .append(stringValue)
          .append(". ");
    }
    return realValue;
  }

  private NewIntervention.OldCustomer.OldCustomerType getCustomerType(
      Row currentRow, StringBuilder exceptionMsgBuilder) {
    int cellIndex = 24;
    String stringValue = getStringValue(currentRow.getCell(cellIndex));
    String realValue = getRealValue(stringValue);
    if (realValue != null && !Arrays.asList(customerType()).contains(realValue)) {
      exceptionMsgBuilder.append(position(currentRow, cellIndex))
          .append(" only support these values ")
          .append(Arrays.toString(customerType()))
          .append(" but was ")
          .append(stringValue)
          .append(". ");
    }
    return stringValue == null ? null
        : stringValue.equals(INDIVIDUAL_VALUE)
        ? NewIntervention.OldCustomer.OldCustomerType.INDIVIDUAL
        : NewIntervention.OldCustomer.OldCustomerType.PROFESSIONAL;
  }

  private String getInterventionType(Row currentRow, StringBuilder exceptionMsgBuilder) {
    int cellIndex = 21;
    String stringValue = getStringValue(currentRow.getCell(cellIndex));
    String realValue = getRealValue(stringValue);
    if (realValue != null && !Arrays.asList(interventionType()).contains(realValue)) {
      exceptionMsgBuilder.append(position(currentRow, cellIndex))
          .append(" only support these values ")
          .append(Arrays.toString(interventionType()))
          .append(" but was ")
          .append(stringValue)
          .append(". ");
    }
    return realValue;
  }

  private static String getRealValue(String stringValue) {
    return stringValue == null ? null : stringValue.toLowerCase().replaceAll("\\s", "");
  }

  public static String[] customerType() {
    return new String[] {"professionnel", "particulier"};
  }

  public static String[] interventionType() {
    return new String[] {"désinsectisation", "désinfection", "dératisation"};
  }

  public static String[] infestationType() {
    return new String[] {"blatte", "fourmi", "puces", "moustiques", "punaise de lit", "termite",
        "guêpe", "frelon", "rat", "souris", "mulot", "surmulot", "autre"};
  }

  public static String[] professionalCustomerType() {
    return new String[] {"commerce", "restaurant", "hangar", "hôtel", "épicerie",
        "boulangerie-patisserie", "école", "établissement de santé", "centres commerciaux",
        "entrepôts et usines", "jardins et parcs publics", "fermes et élevages",
        "centres de loisirs et parcs d'attraction", "installations de production alimentaire",
        "gestionnaire de biens immobiliers", "syndicat de copropriété", "association",
        "administration publique", "bijouterie", "pharmacie", "hangar", "autre"};
  }

  private String getInfestationType(Row currentRow, StringBuilder exceptionMsgBuilder) {
    int cellIndex = 22;
    String stringValue = getStringValue(currentRow.getCell(cellIndex));
    String realValue = getRealValue(stringValue);
    if (!Arrays.asList(infestationType()).contains(realValue)) {
      exceptionMsgBuilder.append(position(currentRow, cellIndex))
          .append(" only support these values ")
          .append(Arrays.toString(infestationType()))
          .append(" but was ")
          .append(realValue)
          .append(". ");
    }
    return realValue;
  }

  private Double getDistRobberyAndOldCustomer(GeoPosition robbPos, String oldCustomerAddress) {
    GeoPosition customerAddressPos = banApi.fSearch(oldCustomerAddress);
    return customerAddressPos == null ? null
        : customerAddressPos.getCoordinates().getDistanceFrom(robbPos.getCoordinates());
  }

  private Double getDistNewIntAndOldCustomer(GeoPosition newIntPos, String oldCustomerAddress) {
    GeoPosition custAddressPos = banApi.fSearch(oldCustomerAddress);
    return custAddressPos == null ? null
        : custAddressPos.getCoordinates().getDistanceFrom(newIntPos.getCoordinates());
  }

  private void setProspectJobValue(StringBuilder exceptionMsgBuilder, Row currentRow,
                                   ProspectEval<NewIntervention> prospectEval) {
    String jobValue = getStringValue(currentRow.getCell(JOB_CELL_INDEX));
    if (jobValue == null) {
      exceptionMsgBuilder
          .append("Job is mandatory but is not present in row-")
          .append(currentRow.getRowNum() + 1);
    } else {
      if (jobValue.equals(ANTI_HORM_VALUE)) {
        prospectEval.setAntiHarm(true);
        prospectEval.setLockSmith(false);
      } else if (jobValue.equals(LOCK_SMITH_VALUE)) {
        prospectEval.setAntiHarm(false);
        prospectEval.setLockSmith(true);
      } else {
        throw new NotImplementedException(
            "Only \"" + ANTI_HORM_VALUE + "\" or \"" + LOCK_SMITH_VALUE
                + "\" is supported for now. Otherwise, " + jobValue + " was given");
      }
    }
  }

  private ProspectEvalInfo getNewProspect(Row currentRow, StringBuilder exceptionMsgBuilder) {
    ProspectEvalInfo prospectEvalInfo = new ProspectEvalInfo();
    for (int colIndex = 0; colIndex < 13; colIndex++) {
      Cell currentCell = currentRow.getCell(colIndex);
      switch (colIndex) {
        case 0:
          prospectEvalInfo.setName(getStringValue(currentCell));
          break;
        case 1:
          prospectEvalInfo.setWebsite(getStringValue(currentCell));
          break;
        case 2:
          prospectEvalInfo.setCategory(getStringValue(currentCell));
          break;
        case 3:
          prospectEvalInfo.setSubcategory(getStringValue(currentCell));
          break;
        //TODO: remove detected Brain Method
        case PROSPECT_ADDRESS_COL_INDEX:
          if (getStringValue(currentCell) == null) {
            throw new BadRequestException(
                "Prospect address is mandatory but not present for row "
                    + (currentRow.getRowNum() + 1));
          } else {
            String addressValue = getStringValue(currentCell);
            prospectEvalInfo.setAddress(addressValue);
            GeoPosition geoPosition = banApi.fSearch(addressValue);
            prospectEvalInfo.setCoordinates(
                geoPosition == null ? null : geoPosition.getCoordinates());
          }
          break;
        case 5:
          prospectEvalInfo.setPhoneNumber(getStringValue(currentCell));
          break;
        case 6:
          prospectEvalInfo.setEmail(getStringValue(currentCell));
          break;
        case 7:
          prospectEvalInfo.setManagerName(getStringValue(currentCell));
          break;
        case 8:
          prospectEvalInfo.setMailSent(getStringValue(currentCell));
          break;
        case 9:
          prospectEvalInfo.setPostalCode(getStringValue(currentCell));
          break;
        case 10:
          prospectEvalInfo.setCity(getStringValue(currentCell));
          break;
        case 11:
          Date dateValue = getDateValue(currentCell);
          prospectEvalInfo.setCompanyCreationDate(dateValue);
          break;
        case 12:
          String natureValue = getStringValue(currentCell);
          if (natureValue == null) {
            prospectEvalInfo.setContactNature(null);
          } else {
            prospectEvalInfo.setContactNature(natureValue.equals(PROSPECT_NATURE_VALUE)
                ? PROSPECT : OTHER);
          }
          break;
        default:
          throw new ApiException(SERVER_EXCEPTION,
              "Unexpected exception occurred when parsing excel values to new newProspect");
      }
    }
    return prospectEvalInfo;
  }

  private String getStringValue(Cell cell) {
    if (cell == null || cell.getCellType() == CELL_TYPE_ERROR) {
      return null;
    }
    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
      return doubleValue(cell) == null
          ? null
          : String.valueOf(Objects.requireNonNullElse(doubleValue(cell), 0).intValue());
    }
    return cell.getStringCellValue().isEmpty() || cell.getStringCellValue().isBlank()
        ? null
        : cell.getStringCellValue();
  }

  private Date getDateValue(Cell cell) {
    if (cell == null || cell.getCellType() == CELL_TYPE_ERROR) {
      return null;
    }
    try {
      return cell.getCellType() == CELL_TYPE_BLANK
          ? null
          : cell.getDateCellValue();
    } catch (IllegalStateException | NumberFormatException e) {
      throw new BadRequestException(
          "Invalid date format when importing new prospect");
    }
  }

  private Double doubleValue(Cell cell) {
    if (cell == null || cell.getCellType() == CELL_TYPE_ERROR) {
      return null;
    }
    try {
      return cell.getCellType() == CELL_TYPE_BLANK ? null : cell.getNumericCellValue();
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Boolean rowBooleanValue(Row row, int cellIndex, StringBuilder exceptionMsgBuilder) {
    String stringValue = getStringValue(row.getCell(cellIndex));
    Boolean bool = stringValue == null ? null : (stringValue.equals("Yes")
        ? Boolean.TRUE : (stringValue.equals("No") ? Boolean.FALSE : null));
    if (bool == null && stringValue != null) {
      exceptionMsgBuilder
          .append(position(row, cellIndex))
          .append(" accepts only `Yes` or `No`")
          .append(" but was ")
          .append(stringValue)
          .append(". ");
    }
    return stringValue == null ? null : bool;
  }

  private String position(Row row, int cellIndex) {
    return "Row-" + (row.getRowNum() + 1) + ",Cell-" + cellIndex;
  }

  private void addMissingException(StringBuilder exceptionMsgBuilder, Row currentRow) {
    exceptionMsgBuilder
        .append(
            "Depa rule (column-N) is mandatory to evaluate prospect but is not present")
        .append(" for row-")
        .append(currentRow.getRowNum())
        .append(". ");
  }
}
