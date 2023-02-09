package app.bpartners.api.service.utils;

import app.bpartners.api.endpoint.rest.model.CreateProduct;
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

public class ProductUtils {
  private ProductUtils() {
  }

  public static List<CreateProduct> getProductsFromFile(InputStream fileToRead) {
    try {
      Workbook workbook = WorkbookFactory.create(fileToRead);
      Sheet sheet = workbook.getSheet(workbook.getSheetName(0));
      List<CreateProduct> createProducts = new ArrayList<>();
      Iterator<Row> rows = sheet.rowIterator();
      int rowIndex = 0;
      while (rows.hasNext()) {
        Row currentRow = rows.next();
        Iterator<Cell> cell = currentRow.cellIterator();
        if (rowIndex == 0) {
          checkColumnOrder(cell);
          rowIndex++;
        } else {
          createProducts.add(mapToProduct(cell));
        }
      }
      workbook.close();
      return createProducts;
    } catch (InvalidFormatException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, "Failed to parse Excel file : " + e.getMessage());
    }
  }

  public static CreateProduct mapToProduct(Iterator<Cell> cell) {
    CreateProduct product = new CreateProduct();
    int cellIndex = 0;

    while (cell.hasNext()) {
      Cell currentCell = cell.next();

      switch (cellIndex) {
        case 0:
          product.setDescription(currentCell.getStringCellValue());
          break;
        case 1:
          product.setQuantity((int) currentCell.getNumericCellValue());
          break;
        case 2:
          product.setUnitPrice((int) currentCell.getNumericCellValue());
          break;
        default:
          product.setVatPercent((int) currentCell.getNumericCellValue());
          break;
      }
      cellIndex++;
    }
    return product;
  }

  private static void checkColumnOrder(Iterator<Cell> cell) {
    int cellIndex = 0;
    StringBuilder message = new StringBuilder();
    while (cell.hasNext()) {
      Cell currentCell = cell.next();
      switch (cellIndex) {
        case 0:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("description")) {
            message.append("\"Description\" instead of ")
                .append("\"")
                .append(currentCell.getStringCellValue())
                .append("\"")
                .append(" at column 1. ");
          }
          break;

        case 1:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("quantité")) {
            message.append("\"Quantité\" instead of ")
                .append("\"")
                .append(currentCell.getStringCellValue())
                .append("\"")
                .append(" at column 2. ");
          }
          break;

        case 2:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("prix unitaire")) {
            message.append("\"Prix unitaire\" instead of ")
                .append("\"")
                .append(currentCell.getStringCellValue())
                .append("\"")
                .append(" at column 3. ");
          }
          break;

        default:
          if (!currentCell.getStringCellValue().equalsIgnoreCase("tva (%)")) {
            message.append("\"TVA (%)\" instead of ")
                .append("\"")
                .append(currentCell.getStringCellValue())
                .append("\"")
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
