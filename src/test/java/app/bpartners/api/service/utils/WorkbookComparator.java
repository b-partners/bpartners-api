package app.bpartners.api.service.utils;

import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hssf.util.CellReference;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public class WorkbookComparator {

  public static boolean isEqual(XSSFWorkbook wb1, XSSFWorkbook wb2) {
    return compare(wb1, wb2).isEmpty();
  }

  private static List<String> compare(XSSFWorkbook wb1, XSSFWorkbook wb2) {
    List<String> differences = new ArrayList<>();

    int sheetCount = wb1.getNumberOfSheets();
    if (sheetCount != wb2.getNumberOfSheets()) {
      differences.add(
          "Nombre de feuilles différent: " + sheetCount + " vs " + wb2.getNumberOfSheets());
    }

    for (int s = 0; s < Math.min(sheetCount, wb2.getNumberOfSheets()); s++) {
      XSSFSheet sheet1 = wb1.getSheetAt(s);
      XSSFSheet sheet2 = wb2.getSheetAt(s);
      compareSheets(sheet1, sheet2, differences);
    }

    return differences;
  }

  private static void compareSheets(XSSFSheet s1, XSSFSheet s2, List<String> diffs) {
    String name = s1.getSheetName();

    int maxRow = Math.max(s1.getLastRowNum(), s2.getLastRowNum());

    for (int r = 0; r <= maxRow; r++) {
      Row row1 = s1.getRow(r);
      Row row2 = s2.getRow(r);

      if (row1 == null && row2 == null) continue;

      int maxCol =
          Math.max(
              row1 != null ? row1.getLastCellNum() : 0, row2 != null ? row2.getLastCellNum() : 0);

      for (int c = 0; c < maxCol; c++) {
        Cell cell1 = row1 != null ? row1.getCell(c) : null;
        Cell cell2 = row2 != null ? row2.getCell(c) : null;

        String val1 = getCellValue(cell1);
        String val2 = getCellValue(cell2);

        if (!val1.equals(val2)) {
          String ref = new CellReference(r, c).formatAsString();
          diffs.add(String.format("[%s] %s : '%s' → '%s'", name, ref, val1, val2));
        }
      }
    }
  }

  private static String getCellValue(Cell cell) {
    if (cell == null) return "";
    return switch (cell.getCellType()) {
      case 4 -> String.valueOf(cell.getBooleanCellValue());
      default -> cell.getStringCellValue();
    };
  }
}
