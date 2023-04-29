package app.bpartners.api.service.utils;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class CustomerUtils {
  private CustomerUtils() {
  }

  public static List<CreateCustomer> getCustomersInfoFromFile(InputStream fileToRead) {
    try {
      Workbook workbook = WorkbookFactory.create(fileToRead);
      Sheet sheet = workbook.getSheet(workbook.getSheetName(0));
      List<CreateCustomer> customerTemplates = new ArrayList<>();
      Iterator<Row> rows = sheet.rowIterator();
      int rowIndex = 0;
      while (rows.hasNext()) {
        Row currentRow = rows.next();
        Iterator<Cell> cell = currentRow.cellIterator();
        //Skip file headers but check column order
        if (rowIndex == 0) {
          verifyColumnOrder(cell);
        } else {
          customerTemplates.add(mapToCustomer(cell));
        }
        rowIndex++;
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

  private static CreateCustomer mapToCustomer(Iterator<Cell> cell) {
    CreateCustomer customer = new CreateCustomer();
    int cellIndex = 0;

    while (cell.hasNext()) {
      Cell currentCell = cell.next();

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

        default:
          customer.setComment(getStringValue(currentCell));
          break;
      }
      cellIndex++;
    }
    return customer;
  }

  private static void verifyColumnOrder(Iterator<Cell> cell) {
    int index = 0;
    StringBuilder messageBuilder = new StringBuilder();
    while (cell.hasNext()) {
      Cell actualCell = cell.next();
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
        default:
          if (!actualCell.getStringCellValue().trim()
              .equalsIgnoreCase("commentaires")) {
            messageBuilder.append("\"Commentaires\" instead of ")
                .append("\"").append(actualCell.getStringCellValue()).append("\"")
                .append(" at the last column.");
          }
          break;
      }
      index++;
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
      return cell.getNumericCellValue();
    } catch (NumberFormatException e) {
      return null;
    }
  }

  @SafeVarargs
  private static <T> Predicate<T> distinctByKeys(final Function<? super T, ?>... keyExtractors) {
    final Map<List<?>, Boolean> seen = new ConcurrentHashMap<>();
    return elt -> {
      final List<?> keys = Arrays.stream(keyExtractors)
          .map(key -> key.apply(elt))
          .collect(Collectors.toList());
      return seen.putIfAbsent(keys, Boolean.TRUE) == null;
    };
  }
}
