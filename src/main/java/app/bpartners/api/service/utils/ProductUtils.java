package app.bpartners.api.service.utils;

import app.bpartners.api.endpoint.rest.model.CreateProduct;
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
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.jsoup.internal.StringUtil.isBlank;

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
      return createProducts.stream()
          .filter(createProduct -> !hasBlankFields(createProduct))
          .filter(distinctByKeys(
              CreateProduct::getDescription
          ))
          .toList();
    } catch (InvalidFormatException | IOException e) {
      throw new ApiException(SERVER_EXCEPTION, "Failed to parse Excel file : " + e.getMessage());
    }
  }

  public static CreateProduct mapToProduct(Iterator<Cell> cell) {
    CreateProduct product = new CreateProduct();
    int cellIndex = 0;

    while (cell.hasNext()) {
      Cell currentCell = cell.next();
      if (currentCell == null) {
        continue;
      } else {
        switch (cellIndex) {
          case 0:
            product.setDescription(currentCell.getStringCellValue());
            break;
          case 1:
            product.setQuantity(getIntValue(currentCell));
            break;
          case 2:
            product.setUnitPrice(getIntValue(currentCell));
            break;
          default:
            product.setVatPercent(getIntValue(currentCell));
            break;
        }
      }
      cellIndex++;
    }
    if (product.getUnitPrice() != null) {
      product.setUnitPrice(product.getUnitPrice() * 100);
    }
    if (product.getVatPercent() != null) {
      product.setVatPercent(product.getVatPercent() * 100);
    }
    return product;
  }

  private static void checkColumnOrder(Iterator<Cell> cell) {
    int index = 0;
    StringBuilder messageBuilder = new StringBuilder();
    while (cell.hasNext()) {
      Cell current = cell.next();
      switch (index) {
        case 0:
          if (!current.getStringCellValue().trim()
              .equalsIgnoreCase("description")) {
            messageBuilder.append("\"Description\" instead of ")
                .append("\"")
                .append(current.getStringCellValue())
                .append("\"")
                .append(" at column 1. ");
          }
          break;

        case 1:
          if (!current.getStringCellValue().trim()
              .equalsIgnoreCase("quantité")) {
            messageBuilder.append("\"Quantité\" instead of ")
                .append("\"")
                .append(current.getStringCellValue())
                .append("\"")
                .append(" at column 2. ");
          }
          break;

        case 2:
          if (!current.getStringCellValue().trim()
              .equalsIgnoreCase("prix unitaire (€)")) {
            messageBuilder.append("\"Prix unitaire (€)\" instead of ")
                .append("\"")
                .append(current.getStringCellValue())
                .append("\"")
                .append(" at column 3. ");
          }
          break;

        default:
          if (!current.getStringCellValue().trim()
              .equalsIgnoreCase("tva (%)")) {
            messageBuilder.append("\"TVA (%)\" instead of ")
                .append("\"")
                .append(current.getStringCellValue())
                .append("\"")
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

  private static boolean hasBlankFields(CreateProduct createProduct) {
    return isBlank(createProduct.getDescription())
        && isEmpty(createProduct.getDescription())
        && isNull(createProduct.getUnitPrice())
        && isNull(createProduct.getVatPercent())
        && isNull(createProduct.getQuantity());
  }

  private static Double doubleValue(Cell cell) {
    try {
      return cell.getNumericCellValue();
    } catch (NumberFormatException e) {
      return null;
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
}
