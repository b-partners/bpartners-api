package app.bpartners.api.service.utils;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static org.apache.commons.lang3.StringUtils.isBlank;

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
          .filter(createCustomer -> !hasBlankFields(createCustomer))
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
          customer.setLastName((String) getCellValue(currentCell));
          break;

        case 1:
          customer.setFirstName((String) getCellValue(currentCell));
          break;

        case 2:
          customer.setEmail((String) getCellValue(currentCell));
          break;

        case 3:
          customer.setPhone((String) getCellValue(currentCell));
          break;

        case 4:
          customer.setWebsite((String) getCellValue(currentCell));
          break;

        case 5:
          customer.setAddress((String) getCellValue(currentCell));
          break;

        case 6:
          customer.setZipCode((int) (double) getCellValue(currentCell));
          break;

        case 7:
          customer.setCity((String) getCellValue(currentCell));
          break;

        case 8:
          customer.setCountry((String) getCellValue(currentCell));
          break;

        default:
          customer.setComment((String) getCellValue(currentCell));
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

  //TODO: Should be improved because it costs too much complexity
  public static List<Customer> removeDuplicate(List<Customer> list) {
    for (int i = 0; i < list.size() - 1; i++) {
      Customer currentCustomer = list.get(i);
      for (int j = 0; j < list.size(); j++) {
        Customer nextCustomer = list.get(j);
        if (currentCustomer.getFirstName().equals(nextCustomer.getFirstName())
            && currentCustomer.getLastName().equals(nextCustomer.getLastName())
            && currentCustomer.getEmail().equals(nextCustomer.getEmail())) {
          list.remove(currentCustomer);
        }
      }
    }
    return list;
  }

  private static boolean hasBlankFields(CreateCustomer customer) {
    return isBlank(customer.getFirstName())
        && isBlank(customer.getLastName());
  }

  private static Object getCellValue(Cell cell) {
    if (cell.getCellType() == Cell.CELL_TYPE_NUMERIC) {
      return cell.getNumericCellValue();
    }
    return cell.getStringCellValue();
  }
}
