package app.bpartners.api.service.utils;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

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
      return customerTemplates;
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
          customer.setLastName(currentCell.getStringCellValue());
          break;

        case 1:
          customer.setFirstName(currentCell.getStringCellValue());
          break;

        case 2:
          customer.setEmail(currentCell.getStringCellValue());
          break;

        case 3:
          customer.setPhone(currentCell.getStringCellValue());
          break;

        case 4:
          customer.setWebsite(currentCell.getStringCellValue());
          break;

        case 5:
          customer.setAddress(currentCell.getStringCellValue());
          break;

        case 6:
          customer.setZipCode((int) currentCell.getNumericCellValue());
          break;

        case 7:
          customer.setCity(currentCell.getStringCellValue());
          break;

        case 8:
          customer.setCountry(currentCell.getStringCellValue());
          break;

        default:
          customer.setComment(currentCell.getStringCellValue());
          break;
      }
      cellIndex++;
    }
    return customer;
  }

  private static void verifyColumnOrder(Iterator<Cell> cell) {
    int cellIndex = 0;
    StringBuilder message = new StringBuilder();
    while (cell.hasNext()) {
      Cell currentCell = cell.next();
      switch (cellIndex) {
        case 0:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("nom")) {
            message.append("\"Nom\" instead of ")
                .append("\"").append(currentCell.getStringCellValue()).append("\"")
                .append(" at column 1. ");
          }
          break;
        case 1:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("prénom(s)")) {
            message.append("\"Prénom(s)\" instead of ")
                .append("\"").append(currentCell.getStringCellValue()).append("\"")
                .append(" at column 2. ");
          }
          break;

        case 2:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("email")) {
            message.append("\"Email\" instead of ")
                .append("\"").append(currentCell.getStringCellValue()).append("\"")
                .append(" at column 3. ");
          }
          break;

        case 3:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("téléphone")) {
            message.append("\"Téléphone\" instead of ")
                .append("\"").append(currentCell.getStringCellValue()).append("\"")
                .append(" at column 4. ");
          }
          break;

        case 4:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("siteweb")) {
            message.append("\"Siteweb\" instead of ")
                .append("\"").append(currentCell.getStringCellValue()).append("\"")
                .append(" at column 5. ");
          }
          break;

        case 5:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("adresse")) {
            message.append("\"Adresse\" instead of ")
                .append("\"").append(currentCell.getStringCellValue()).append("\"")
                .append(" at column 6. ");
          }
          break;

        case 6:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("code postal")) {
            message.append("\"Code Postal\" instead of ")
                .append("\"").append(currentCell.getStringCellValue()).append("\"")
                .append(" at column 7. ");
          }
          break;

        case 7:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("ville")) {
            message.append("\"Ville\" instead of ")
                .append("\"").append(currentCell.getStringCellValue()).append("\"")
                .append(" at column 8. ");
          }
          break;

        case 8:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("pays")) {
            message.append("\"Pays\" instead of ")
                .append("\"").append(currentCell.getStringCellValue()).append("\"")
                .append(" at column 9. ");
          }
          break;

        default:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("commentaires")) {
            message.append("\"Commentaires\" instead of ")
                .append("\"").append(currentCell.getStringCellValue()).append("\"")
                .append(" at the last column.");
          }
          break;
      }
      cellIndex++;
    }
    String errorMessage = message.toString();
    if (!errorMessage.isEmpty()) {
      throw new BadRequestException(errorMessage);
    }
  }
}
