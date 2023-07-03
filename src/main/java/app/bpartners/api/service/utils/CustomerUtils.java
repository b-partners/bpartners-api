package app.bpartners.api.service.utils;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.FilterUtils.distinctByKeys;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class CustomerUtils {
  private static final int FIRST_SHEET_INDEX = 0;

  private CustomerUtils() {
  }

  public static List<CreateCustomer> getCustomersInfoFromFile(InputStream fileToRead) {
    try {
      Workbook workbook = WorkbookFactory.create(fileToRead);
      Sheet sheet = workbook.getSheetAt(FIRST_SHEET_INDEX);
      List<CreateCustomer> customerTemplates = new ArrayList<>();
      Iterator<Row> rows = sheet.rowIterator();
      while (rows.hasNext()) {
        Row currentRow = rows.next();
        if (currentRow.getRowNum() == 0) {
          verifyColumnOrder(currentRow);
        } else {
          customerTemplates.add(mapToCustomer(currentRow));
        }
      }
      workbook.close();
      return customerTemplates.stream()
          //Remove blank fields
          .filter(createCustomer -> !hasBlankFields(createCustomer))
          //Remove duplicated customer
          .filter(distinctByKeys(
              CreateCustomer::getFirstName,
              CreateCustomer::getLastName,
              CreateCustomer::getEmail))
          .collect(Collectors.toUnmodifiableList());
    } catch (InvalidFormatException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, "Failed to parse Excel file : " + e.getMessage());
    }
  }

  private static CreateCustomer mapToCustomer(Row currentRow) {
    CreateCustomer customer = new CreateCustomer();

    for (int cellIndex = 0; cellIndex < 10; cellIndex++) {
      Cell currentCell = currentRow.getCell(cellIndex);
      switch (cellIndex) {
        case 0:
          customer.setLastName(getStringValue(currentCell));
          break;
        case 1:
          customer.setFirstName(getStringValue(currentCell));
          break;
        case 2:
          customer.setEmail(getStringValue(currentCell));
          break;
        case 3:
          customer.setPhone(getStringValue(currentCell));
          break;
        case 4:
          customer.setWebsite(getStringValue(currentCell));
          break;
        case 5:
          customer.setAddress(getStringValue(currentCell));
          break;
        case 6:
          customer.setZipCode(getIntValue(currentCell));
          break;
        case 7:
          customer.setCity(getStringValue(currentCell));
          break;
        case 8:
          customer.setCountry(getStringValue(currentCell));
          break;
        case 9:
          customer.setComment(getStringValue(currentCell));
          break;
        default:
          throw new ApiException(SERVER_EXCEPTION,
              "Unexpected exception occurred when parsing excel values to new customers");
      }
    }
    return customer;
  }

  private static void verifyColumnOrder(Row currentRow) {
    StringBuilder messageBuilder = new StringBuilder();
    for (int index = 0; index < 10; index++) {
      Cell actualCell = currentRow.getCell(index);
      switch (index) {
        case 0:
          if (!actualCell.getStringCellValue().trim()
              .equalsIgnoreCase("nom")) {
            messageBuilder.append("\"Nom\" instead of ")
                .append("\"").append(actualCell.getStringCellValue()).append("\"")
                .append(" at column 1. ");
          }
          break;
        case 1:
          if (!actualCell.getStringCellValue().trim()
              .equalsIgnoreCase("prénom(s)")) {
            messageBuilder.append("\"Prénom(s)\" instead of ")
                .append("\"").append(actualCell.getStringCellValue()).append("\"")
                .append(" at column 2. ");
          }
          break;
        case 2:
          if (!actualCell.getStringCellValue().trim()
              .equalsIgnoreCase("email")) {
            messageBuilder.append("\"Email\" instead of ")
                .append("\"").append(actualCell.getStringCellValue()).append("\"")
                .append(" at column 3. ");
          }
          break;
        case 3:
          if (!actualCell.getStringCellValue().trim()
              .equalsIgnoreCase("téléphone")) {
            messageBuilder.append("\"Téléphone\" instead of ")
                .append("\"").append(actualCell.getStringCellValue()).append("\"")
                .append(" at column 4. ");
          }
          break;
        case 4:
          if (!actualCell.getStringCellValue().trim()
              .equalsIgnoreCase("siteweb")) {
            messageBuilder.append("\"Siteweb\" instead of ")
                .append("\"").append(actualCell.getStringCellValue()).append("\"")
                .append(" at column 5. ");
          }
          break;
        case 5:
          if (!actualCell.getStringCellValue().trim()
              .equalsIgnoreCase("adresse")) {
            messageBuilder.append("\"Adresse\" instead of ")
                .append("\"").append(actualCell.getStringCellValue()).append("\"")
                .append(" at column 6. ");
          }
          break;
        case 6:
          if (!actualCell.getStringCellValue().trim()
              .equalsIgnoreCase("code postal")) {
            messageBuilder.append("\"Code Postal\" instead of ")
                .append("\"").append(actualCell.getStringCellValue()).append("\"")
                .append(" at column 7. ");
          }
          break;
        case 7:
          if (!actualCell.getStringCellValue().trim()
              .equalsIgnoreCase("ville")) {
            messageBuilder.append("\"Ville\" instead of ")
                .append("\"").append(actualCell.getStringCellValue()).append("\"")
                .append(" at column 8. ");
          }
          break;
        case 8:
          if (!actualCell.getStringCellValue().trim()
              .equalsIgnoreCase("pays")) {
            messageBuilder.append("\"Pays\" instead of ")
                .append("\"").append(actualCell.getStringCellValue()).append("\"")
                .append(" at column 9. ");
          }
          break;
        case 9:
          if (!actualCell.getStringCellValue().trim()
              .equalsIgnoreCase("commentaires")) {
            messageBuilder.append("\"Commentaires\" instead of ")
                .append("\"").append(actualCell.getStringCellValue()).append("\"")
                .append(" at the last column.");
          }
          break;
        default:
          throw new ApiException(SERVER_EXCEPTION, "Unknown error occurred when treating cell at "
              + "index " + index + " of row-" + currentRow.getRowNum());
      }
    }
    String errorMessage = messageBuilder.toString();
    if (!errorMessage.isEmpty()) {
      throw new BadRequestException(errorMessage);
    }
  }

  private static boolean hasBlankFields(CreateCustomer customer) {
    return isBlank(customer.getFirstName())
        && isBlank(customer.getLastName())
        && isEmpty(customer.getFirstName())
        && isEmpty(customer.getLastName());
  }

  private static String getStringValue(Cell cell) {
    if (cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
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

  private static Integer getIntValue(Cell cell) {
    if (cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
      return null;
    }
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
    if (cell == null || cell.getCellType() == Cell.CELL_TYPE_BLANK) {
      return null;
    }
    try {
      return cell.getNumericCellValue();
    } catch (NumberFormatException e) {
      return null;
    }
  }

  public static void checkUniqueEmailConstraint(List<CreateCustomer> customersFromFile) {
    StringBuilder builder = new StringBuilder();
    for (int i = 0; i < customersFromFile.size(); i++) {
      CreateCustomer c1 = customersFromFile.get(i);
      for (int j = i + 1; j < customersFromFile.size(); j++) {
        CreateCustomer c2 = customersFromFile.get(j);
        if (c1.getEmail() != null && c2.getEmail() != null
            && c1.getEmail().equals(c2.getEmail())) {
          builder.append(describeCreateCustomer(c1))
              .append(" and ")
              .append(describeCreateCustomer(c2))
              .append(" have the same email = " + c1.getEmail())
              .append(". ");
          break;
        }
      }
    }
    String message = builder.toString();
    if (!message.isEmpty()) {
      throw new BadRequestException("Email must be unique for each customer.Otherwise,"
          + message);
    }
  }

  public static String describeCreateCustomer(CreateCustomer c) {
    return "Customer(name=" + c.getFirstName() + " " + c.getLastName() + ")";
  }
}
