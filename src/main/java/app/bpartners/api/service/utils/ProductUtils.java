package app.bpartners.api.service.utils;

import app.bpartners.api.endpoint.rest.model.CreateProduct;
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

public class ProductUtils {

  private ProductUtils() {
  }

  public static Optional<String> getFileExtension(String filename) {
    return Optional.ofNullable(filename).filter(f -> f.contains("."))
        .map(f -> f.substring(filename.lastIndexOf(".") + 1));
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
        if (rowIndex == 0) {
          rowIndex++;
        } else {
          Iterator<Cell> cell = currentRow.cellIterator();
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
}
