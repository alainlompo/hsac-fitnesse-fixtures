package nl.hsac.fitnesse.fixture.util.selenium.by;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

/**
 * Custom By to deal with finding elements in a table representing a grid of values.
 */
public abstract class GridBy extends SingleElementOrNullBy {
    public static By coordinates(int columnIndex, int rowIndex) {
        return new ValueAtCoordinates(columnIndex, rowIndex);
    }

    public static By columnInRow(String requestedColumnName, int rowIndex) {
        return new ValueOfInRowNumber(requestedColumnName, rowIndex);
    }

    public static By columnInRowWhereIs(String requestedColumnName, String selectOnColumn, String selectOnValue) {
        return new ValueOfInRowWhereIs(requestedColumnName, selectOnValue, selectOnColumn);
    }

    public static class ValueAtCoordinates extends GridBy {
        private final int columnIndex;
        private final int rowIndex;

        public ValueAtCoordinates(int columnIndex, int rowIndex) {
            this.columnIndex = columnIndex;
            this.rowIndex = rowIndex;
        }

        @Override
        public WebElement findElement(SearchContext context) {
            String row = Integer.toString(rowIndex);
            String column = Integer.toString(columnIndex);
            return getValueByXPath(context, "(.//tr[boolean(td)])[%s]/td[%s]", row, column);
        }
    }

    public static class ValueOfInRowNumber extends GridBy {
        private final String requestedColumnName;
        private final int rowIndex;

        public ValueOfInRowNumber(String requestedColumnName, int rowIndex) {
            this.requestedColumnName = requestedColumnName;
            this.rowIndex = rowIndex;
        }

        @Override
        public WebElement findElement(SearchContext context) {
            String columnXPath = String.format("((.//table[.//tr/th/descendant-or-self::text()[normalized(.)='%s']])[last()]//tr[boolean(td)])[%s]/td", requestedColumnName, rowIndex);
            return valueInRow(context, columnXPath, requestedColumnName);
        }
    }

    public static class ValueOfInRowWhereIs extends GridBy {
        private final String requestedColumnName;
        private final String selectOnColumn;
        private final String selectOnValue;

        public ValueOfInRowWhereIs(String requestedColumnName, String selectOnValue, String selectOnColumn) {
            this.requestedColumnName = requestedColumnName;
            this.selectOnColumn = selectOnColumn;
            this.selectOnValue = selectOnValue;
        }

        @Override
        public WebElement findElement(SearchContext context) {
            String columnXPath = getXPathForColumnInRowByValueInOtherColumn(requestedColumnName, selectOnColumn, selectOnValue);
            return valueInRow(context, columnXPath, requestedColumnName);
        }
    }

    protected WebElement valueInRow(SearchContext context, String columnXPath, String requestedColumnName) {
        String requestedIndex = getXPathForColumnIndex(requestedColumnName);
        return getValueByXPath(context, "%s[%s]", columnXPath, requestedIndex);
    }

    protected WebElement getValueByXPath(SearchContext context, String xpathPattern, String... params) {
        By xPathBy = new XPathBy(xpathPattern, params);
        return new ValueOfBy(xPathBy).findElement(context);
    }

    /**
     * Creates an XPath expression that will find a cell in a row, selecting the row based on the
     * text in a specific column (identified by its header text).
     * @param extraColumnName name of other header text that must be present in table's header row
     * @param columnName header text of the column to find value in.
     * @param value text to find in column with the supplied header.
     * @return XPath expression selecting a td in the row
     */
    public static String getXPathForColumnInRowByValueInOtherColumn(String extraColumnName, String columnName, String value) {
        String selectIndex = getXPathForColumnIndex(columnName);
        return String.format("(.//table[.//tr[th/descendant-or-self::text()[normalized(.)='%3$s'] and th/descendant-or-self::text()[normalized(.)='%4$s']]])[last()]//tr[td[%1$s]/descendant-or-self::text()[normalized(.)='%2$s']]/td",
                selectIndex, value, columnName, extraColumnName);
    }

    /**
     * Creates an XPath expression that will determine, for a row, which index to use to select the cell in the column
     * with the supplied header text value.
     * @param columnName name of column in header (th)
     * @return XPath expression which can be used to select a td in a row
     */
    public static String getXPathForColumnIndex(String columnName) {
        // determine how many columns are before the column with the requested name
        // the column with the requested name will have an index of the value +1 (since XPath indexes are 1 based)
        return String.format("count(ancestor::table[1]//tr/th/descendant-or-self::text()[normalized(.)='%s']/ancestor-or-self::th[1]/preceding-sibling::th)+1", columnName);
    }
}
