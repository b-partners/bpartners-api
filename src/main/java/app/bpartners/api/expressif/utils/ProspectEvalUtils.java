package app.bpartners.api.expressif.utils;

import app.bpartners.api.expressif.NewProspect;
import app.bpartners.api.expressif.ProspectEval;
import app.bpartners.api.expressif.fact.NewIntervention;
import app.bpartners.api.expressif.fact.Robbery;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import static app.bpartners.api.expressif.NewProspect.ContactNature.OTHER;
import static app.bpartners.api.expressif.NewProspect.ContactNature.PROSPECT;
import static app.bpartners.api.expressif.fact.NewIntervention.OldCustomer.OldCustomerType.INDIVIDUAL;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.apache.poi.ss.usermodel.Cell.CELL_TYPE_BLANK;

@Slf4j
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

  private ProspectEvalUtils() {
  }

  @SneakyThrows
  public static List<ProspectEval> convertFromExcel(InputStream file) {
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

          setProspectJobValue(exceptionMsgBuilder, currentRow, prospectEval);
          prospectEval.setNewProspect(getNewProspect(currentRow));

          prospectEval.setInsectControl(rowBooleanValue(currentRow, 15));
          prospectEval.setDisinfection(rowBooleanValue(currentRow, 16));
          prospectEval.setRatRemoval(rowBooleanValue(currentRow, 17));
          prospectEval.setParticularCustomer(rowBooleanValue(currentRow, 18));
          prospectEval.setProfessionalCustomer(rowBooleanValue(currentRow, 19));

          if (depaRuleValue.equals(DEPA_RULE_NEW_INTERVENTION)) {
            String oldCustomerType = getStringValue(currentRow.getCell(25));
            NewIntervention.OldCustomer.OldCustomerType customerType =
                oldCustomerType == null ? null :
                    (oldCustomerType.equals(INDIVIDUAL_VALUE)
                        ? INDIVIDUAL
                        : NewIntervention.OldCustomer.OldCustomerType.PROFESSIONAL);
            prospectEval.setDepaRule(NewIntervention.builder()
                .planned(rowBooleanValue(currentRow, 20))
                .interventionType(getStringValue(currentRow.getCell(21)))
                .infestationType(getStringValue(currentRow.getCell(22)))
                .newIntAddress(getStringValue(currentRow.getCell(23)))
                .distNewIntAndProspect(doubleValue(currentRow.getCell(24)))
                .oldCustomerFact(NewIntervention.OldCustomer.builder()
                    .type(customerType)
                    .professionalType(getStringValue(currentRow.getCell(26)))
                    .oldCustomerAddress(getStringValue(currentRow.getCell(27)))
                    .distNewIntAndOldCustomer(doubleValue(currentRow.getCell(28)))
                    .build())
                .build());
          } else if (depaRuleValue.equals(DEPA_RULE_ROBBERY)) {
            String robberryAddress = getStringValue(currentRow.getCell(32));
            Double distRobberyAndOldCustomer = doubleValue(currentRow.getCell(33));
            prospectEval.setDepaRule(Robbery.builder()
                .declared(rowBooleanValue(currentRow, 29))
                .robberyAddress(getStringValue(currentRow.getCell(30)))
                .distRobberyAndProspect(doubleValue(currentRow.getCell(31)))
                .oldCustomer((robberryAddress == null
                    && (distRobberyAndOldCustomer == null || distRobberyAndOldCustomer == 0.0))
                    ? null
                    : Robbery.OldCustomer.builder()
                    .address(robberryAddress)
                    .distRobberyAndOldCustomer(distRobberyAndOldCustomer)
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
    String exceptionMsg = exceptionMsgBuilder.toString();
    if (!exceptionMsg.isEmpty()) {
      throw new BadRequestException(exceptionMsg);
    }
    return prospectEvalList;
  }

  private static void setProspectJobValue(StringBuilder exceptionMsgBuilder, Row currentRow,
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

  private static NewProspect getNewProspect(Row currentRow) {
    NewProspect newProspect = new NewProspect();
    for (int colIndex = 0; colIndex < 13; colIndex++) {
      Cell currentCell = currentRow.getCell(colIndex);
      switch (colIndex) {
        case 0:
          newProspect.setName(getStringValue(currentCell));
          break;
        case 1:
          newProspect.setWebsite(getStringValue(currentCell));
          break;
        case 2:
          newProspect.setCategory(getStringValue(currentCell));
          break;
        case 3:
          newProspect.setSubcategory(getStringValue(currentCell));
          break;
        case 4:
          newProspect.setAddress(getStringValue(currentCell));
          break;
        case 5:
          newProspect.setPhoneNumber(getStringValue(currentCell));
          break;
        case 6:
          newProspect.setEmail(getStringValue(currentCell));
          break;
        case 7:
          newProspect.setManagerName(getStringValue(currentCell));
          break;
        case 8:
          newProspect.setMailSent(rowBooleanValue(currentRow, 8));
          break;
        case 9:
          newProspect.setPostalCode(getStringValue(currentCell));
          break;
        case 10:
          newProspect.setCity(getStringValue(currentCell));
          break;
        case 11:
          Date dateValue = getDateValue(currentCell);
          newProspect.setCompanyCreationDate(dateValue);
          break;
        case 12:
          String natureValue = getStringValue(currentCell);
          if (natureValue == null) {
            newProspect.setContactNature(null);
          } else {
            newProspect.setContactNature(natureValue.equals(PROSPECT_NATURE_VALUE)
                ? PROSPECT : OTHER);
          }
          break;
        default:
          throw new ApiException(SERVER_EXCEPTION,
              "Unexpected exception occurred when parsing excel values to new newProspect");
      }
    }
    return newProspect;
  }

  private static String getStringValue(Cell cell) {
    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
      return doubleValue(cell) == null
          ? null
          : String.valueOf(Objects.requireNonNullElse(doubleValue(cell), 0).intValue());
    }
    return cell.getStringCellValue().isEmpty() || cell.getStringCellValue().isBlank()
        ? null
        : cell.getStringCellValue();
  }

  private static Date getDateValue(Cell cell) {
    try {
      return cell.getCellType() == CELL_TYPE_BLANK
          ? null
          : cell.getDateCellValue();
    } catch (IllegalStateException | NumberFormatException e) {
      throw new BadRequestException(
          "Invalid date format when importing new prospect");
    }
  }

  private static Integer getIntValue(Cell cell) {
    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
      return doubleValue(cell) == null
          ? null
          : Objects.requireNonNullElse(doubleValue(cell), 0).intValue();
    }
    try {
      return Integer.parseInt(cell.getStringCellValue());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Double doubleValue(Cell cell) {
    try {
      return cell.getCellType() == CELL_TYPE_BLANK ? null : cell.getNumericCellValue();
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static Boolean rowBooleanValue(Row row, int cellIndex) {
    String stringValue = getStringValue(row.getCell(cellIndex));
    return stringValue == null ? null : stringValue.equals("Yes");
  }

  private static void addMissingException(StringBuilder exceptionMsgBuilder, Row currentRow) {
    exceptionMsgBuilder
        .append(
            "Depa rule (column-N) is mandatory to evaluate prospect but is not present")
        .append(" for row-")
        .append(currentRow.getRowNum())
        .append(". ");
  }
}
