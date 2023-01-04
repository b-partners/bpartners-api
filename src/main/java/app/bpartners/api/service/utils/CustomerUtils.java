package app.bpartners.api.service.utils;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.model.exception.ApiException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
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

  public static Optional<String> getExtension(String filename) {
    return Optional.ofNullable(filename)
        .filter(f -> f.contains("."))
        .map(f -> f.substring(filename.lastIndexOf(".") + 1));
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
        //Skip file headers
        if (rowIndex == 0) {
          rowIndex++;
        } else {
          Iterator<Cell> cell = currentRow.cellIterator();
          customerTemplates.add(mapToCustomer(cell));
        }
      }
      workbook.close();
      return customerTemplates;
    } catch (InvalidFormatException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, "Failed to parse Excel file : " + e.getMessage());
    }
  }

  public static CreateCustomer mapToCustomer(Iterator<Cell> cell) {
    CreateCustomer customer = new CreateCustomer();
    int cellIndex = 0;

    while (cell.hasNext()) {
      Cell currentCell = cell.next();

      switch (cellIndex) {
        case 0:
          customer.setName(currentCell.getStringCellValue());
          break;

        case 1:
          customer.setEmail(currentCell.getStringCellValue());
          break;

        case 2:
          customer.setPhone(currentCell.getStringCellValue());
          break;

        case 3:
          customer.setWebsite(currentCell.getStringCellValue());
          break;

        case 4:
          customer.setAddress(currentCell.getStringCellValue());
          break;

        case 5:
          customer.setZipCode((int) currentCell.getNumericCellValue());
          break;

        case 6:
          customer.setCity(currentCell.getStringCellValue());
          break;

        case 7:
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
}
