package org.labkey.test.components.ui.grids;

import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.jetbrains.annotations.Nullable;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.ReactDateTimePicker;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.entities.EntityBulkInsertDialog;
import org.labkey.test.components.ui.entities.EntityBulkUpdateDialog;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static org.labkey.test.BaseWebDriverTest.WAIT_FOR_JAVASCRIPT;
import static org.labkey.test.WebDriverWrapper.waitFor;
import static org.labkey.test.util.TestLogger.log;
import static org.labkey.test.util.selenium.WebDriverUtils.MODIFIER_KEY;

public class EditableGrid extends WebDriverComponent<EditableGrid.ElementCache>
{
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    public static final String SELECT_COLUMN_HEADER = "<select>";
    public static final String ROW_NUMBER_COLUMN_HEADER = "<row number>";

    private final WebElement _gridElement;
    private final WebDriver _driver;

    protected EditableGrid(WebElement editableGrid, WebDriver driver)
    {
        _gridElement = editableGrid;
        _driver = driver;
    }

    protected EditableGrid(EditableGrid wrappedGrid)
    {
        this(wrappedGrid.getComponentElement(), wrappedGrid.getDriver());
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _gridElement;
    }

    public void waitForLoaded()
    {
        Locators.loadingGrid.waitForElementToDisappear(this, 30000);
        Locators.spinner.waitForElementToDisappear(this, 30000);
    }

    public void clickRemove()
    {
        doAndWaitForUpdate(() -> elementCache().deleteRowsBtn.click());
    }

    public EntityBulkInsertDialog clickBulkInsert()
    {
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().bulkInsertBtn));
        elementCache().bulkInsertBtn.click();

        return new EntityBulkInsertDialog(getDriver());
    }

    public EntityBulkUpdateDialog clickBulkUpdate()
    {
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().bulkUpdateBtn));
        elementCache().bulkUpdateBtn.click();

        return new EntityBulkUpdateDialog(getDriver());
    }

    public ExportMenu getExportMenu()
    {
        return elementCache().exportMenu;
    }

    public List<String> getColumnNames()
    {
        return elementCache().getColumnNames();
    }

    protected Integer getColumnIndex(String columnHeader)
    {
        List<String> columnTexts = getColumnNames();
        for (int i=0; i< columnTexts.size(); i++ )
        {
            if (columnTexts.get(i).equalsIgnoreCase(columnHeader))
                return i;
        }
        throw new NotFoundException("Column not found in grid: " + columnHeader + ". Found: " + columnTexts);
    }

    private boolean hasSelectColumn()
    {
        return elementCache().selectColumn.isDisplayed();
    }

    public EditableGrid selectRow(int index, boolean checked)
    {
        if (hasSelectColumn())
        {
            WebElement checkBox = Locator.css("td > input[type=checkbox]").findElement(getRow(index));
            getWrapper().setCheckbox(checkBox, checked);
        }
        else
        {
            throw new NoSuchElementException("There is no select checkbox for row " + index);
        }
        return this;
    }

    public boolean isRowSelected(int index)
    {
        if (hasSelectColumn())
        {
            WebElement checkBox = Locator.css("td > input[type=checkbox]").findElement(getRow(index));
            return checkBox.isSelected();
        }
        else
        {
            throw new NoSuchElementException("There is no select checkbox for row " + index);
        }
    }

    public EditableGrid selectAll(boolean checked)
    {
        if (hasSelectColumn())
        {
            getWrapper().setCheckbox(elementCache().selectColumn, checked);
        }
        else
        {
            throw new NoSuchElementException("There is no select checkbox for all rows.");
        }
        return this;
    }

    public boolean areAllRowsSelected()
    {
        if (hasSelectColumn())
            return new Checkbox(elementCache().selectColumn).isSelected();
        else
            throw new NoSuchElementException("There is no select checkbox for all rows.");
    }

    /**
     * Selects a range of rows in the current view.
     * If the range is within a range of already-selected rows, will deselect the specified range
     * @param start the starting index (0-based), of non-header rows with checkboxes
     * @param end the ending index
     * @return  the current instance
     */
    public EditableGrid shiftSelectRange(int start, int end)
    {
        if (!hasSelectColumn())
            throw new NoSuchElementException("there is no select checkbox for all rows");

        var checkBoxes = Locator.tag("tr").child("td")
                .child(Locator.tagWithAttribute("input", "type", "checkbox"))
                .findElements(elementCache().table);
        getWrapper().scrollIntoView(checkBoxes.get(0), true); // bring as much of the grid into view as possible
        new Actions(getDriver())
                .click(checkBoxes.get(start))
                .keyDown(Keys.SHIFT)
                .click(checkBoxes.get(end))
                .keyUp(Keys.SHIFT)
                .perform();
        return this;
    }

    private List<WebElement> getRows()
    {
        waitForLoaded();
        return Locators.rows.findElements(elementCache().table);
    }

    public List<Map<String, String>> getGridData(String... columns)
    {
        List<Map<String, String>> gridData = new ArrayList<>();

        List<String> columnNames = getColumnNames();
        Set<Integer> includedColIndices = new HashSet<>();
        if (columns.length > 0)
        {
            Assertions.assertThat(columnNames).as("Editable grid columns").contains(columns);
            for (String col : columns)
            {
                int colIndex = columnNames.indexOf(col);
                includedColIndices.add(colIndex);
            }
        }

        for (WebElement row : getRows())
        {
            List<WebElement> cells = row.findElements(By.tagName("td"));
            Map<String, String> rowMap = new HashMap<>();

            for (int i = 0; i < cells.size(); i++)
            {
                if (includedColIndices.isEmpty() || includedColIndices.contains(i))
                {
                    WebElement cell = cells.get(i);
                    String columnName = columnNames.get(i);

                    if (columnName.equals(SELECT_COLUMN_HEADER))
                    {
                        rowMap.put(columnName, String.valueOf(cell.findElement(By.tagName("input")).isSelected()));
                    }
                    else
                    {
                        rowMap.put(columnName, cell.getText());
                    }
                }
            }

            gridData.add(rowMap);
        }

        return gridData;
    }

    public List<String> getColumnData(String columnLabel)
    {
        return getGridData(columnLabel).stream().map(a-> a.get(columnLabel)).collect(Collectors.toList());
    }

    private WebElement getRow(int index)
    {
        return getRows().get(index);
    }

    /**
     * Find the first row index containing the text value in the given column.
     * If not found -1 is returned.
     *
     * @param columnLabel Column label to look at.
     * @param text Text to look for (must match exactly).
     * @return The first row index where found, -1 if not found.
     */
    public Integer getRowIndex(String columnLabel, String text)
    {
        int index = -1;

        List<String> columnData = getColumnData(columnLabel);
        for(int i = 0; i < columnData.size(); i++)
        {
            if(columnData.get(i).equals(text))
            {
                index = i;
                break;
            }
        }

        return index;
    }


    /**
     * Get the td element for a cell.
     *
     * @param row The 0 based row index.
     * @param column The name of the column to get the cell.
     * @return A {@link WebElement} that is the td for the cell.
     */
    public WebElement getCell(int row, String column)
    {
        int columNumber = getColumnIndex(column) + 1;
        return Locator.css("td:nth-of-type(" + columNumber + ")").findElement(getRow(row));
    }

    public boolean isCellReadOnly(int row, String column)
    {
        WebElement div = Locator.tag("div").findElement(getCell(row, column));
        String cellClass = div.getAttribute("class");
        return cellClass != null && cellClass.contains("cell-read-only");
    }

    public int getRowCount()
    {
        return getRows().size();
    }

    /**
     * As best as possible get a list of row indices from the grid for editable rows. That is rows where the values can
     * be entered or changed.
     *
     * @return A list of indices (0 based) for rows that can be edited.
     */
    public List<Integer> getEditableRowIndices()
    {
        return getRowTypes().get(0);
    }

    /**
     * Some EditableGrids have read only rows. These are rows in the grid that display data but cannot be updated. As
     * best as possible return a list of those rows.
     *
     * @return A list of indices (0 based) of rows that cannot be edited.
     */
    public List<Integer> getReadonlyRowIndices()
    {
        return getRowTypes().get(1);
    }

    private List<List<Integer>> getRowTypes()
    {
        List<Integer> unPopulatedRows = new ArrayList<>();
        List<Integer> populatedRows = new ArrayList<>();

        // Need to look at an attribute of a cell to see if it has pre-populated data.
        // But this info will not be in the select or row-number cells, so need to find a cell other than that.
        int checkColumn = getColumnNames().size() - 1;
        int rowCount = 0;

        for (WebElement row : getRows())
        {
            String classAttribute = row.findElement(By.cssSelector("td:nth-child(" + checkColumn + ") > div"))
                    .getAttribute("class");

            if ((!classAttribute.contains("cell-selection")) && (!classAttribute.contains("cell-read-only")))
            {
                unPopulatedRows.add(rowCount);
            }
            else
            {
                populatedRows.add(rowCount);
            }

            rowCount++;
        }

        return new ArrayList<>(Arrays.asList(unPopulatedRows, populatedRows));
    }

    /**
     * <p>
     *     For a given column, 'columnNameToSet', set the cell in the row if value in column 'columnNameToSearch'
     *     equals 'valueToSearch'.
     * </p>
     * <p>
     *     Rather than set one cell in a specific row, this function will loop through all the rows in the grid and
     *     will update the value in column 'columnNameToSet' only if the value in the column 'columnNameToSearch' equal
     *     'valueToSearch' in that row.
     * </p>
     * <p>
     *     The check for equality for 'valueToSearch' is case sensitive.
     * </p>
     *
     * @param columnNameToSearch The name of the column to check if a row should be updated or not.
     * @param valueToSearch The value to check for in 'columnNameToSearch' to see if the row should be updated.
     * @param columnNameToSet The column to update in a row.
     * @param valueToSet The new value to put into column 'columnNameToSet'.
     */
    public void setCellValue(String columnNameToSearch, String valueToSearch, String columnNameToSet, Object valueToSet)
    {
        setCellValue(getRowIndex(columnNameToSearch, valueToSearch), columnNameToSet, valueToSet);
    }

    /**
     * <p>
     * For the identified row set the value in the identified column.
     * </p>
     * <p>
     * If the column to be updated is a look-up, the value passed in must be a list, even if it is just one value.
     * This is needed so the function knows how to set the value.
     * </p>
     *
     * @param row        Index of the row (0 based).
     * @param columnName Name of the column to update.
     * @param value      If the cell is a lookup, value should be List.of(value(s)). To use the date picker pass a 'Date', 'LocalDate', or 'LocalDateTime'
     * @return cell WebElement
     */
    public WebElement setCellValue(int row, String columnName, Object value)
    {
        return setCellValue(row, columnName, value, true);
    }

    /**
     * <p>
     * For the identified row set the value in the identified column.
     * </p>
     * <p>
     * If the column to be updated is a look-up, the value passed in must be a list, even if it is just one value.
     * This is needed so the function knows how to set the value.
     * </p>
     *
     * @param row        Index of the row (0 based).
     * @param columnName Name of the column to update.
     * @param value      If the cell is a lookup, value should be List.of(value(s)). To use the date picker pass a 'Date', 'LocalDate', or 'LocalDateTime'
     * @param checkContains Check to see if the value passed in is contained in the value shown in the grid after the edit.
     *                   Will be true most of the time but can be false if the field has formatting that may alter the value passed in like date values.
     * @return cell WebElement
     */
    public WebElement setCellValue(int row, String columnName, Object value, boolean checkContains)
    {
        // Normalize date values
        if (value instanceof Date date)
        {
            value = LocalDateTime.ofInstant(date.toInstant(), TimeZone.getDefault().toZoneId());
        }

        WebElement gridCell = selectCell(row, columnName);

        if (value instanceof List)
        {
            // If this is a list assume that it will need a lookup.
            List<String> values = (List) value;

            ReactSelect lookupSelect = elementCache().lookupSelect(gridCell);

            lookupSelect.open();

            for (String _value : values)
            {
                lookupSelect.typeOptionThenSelect(_value);
            }

        }
        else if (value instanceof LocalDateTime localDateTime)
        {
            // Activate the cell.
            activateCell(gridCell);

            ReactDateTimePicker dateTimePicker = elementCache().datePicker();
            dateTimePicker.select(localDateTime);
        }
        else if(value instanceof LocalDate localDate)
        {
            activateCell(gridCell);
            ReactDateTimePicker datePicker = elementCache().datePicker();
            datePicker.selectDate(localDate);
        }
        else if(value instanceof LocalTime localTime)
        {
            activateCell(gridCell);
            ReactDateTimePicker datePicker = elementCache().datePicker();
            datePicker.selectTime(localTime);
        }
        else
        {
            String beforeText = gridCell.getText();

            new Actions(getDriver()).sendKeys(" ").perform(); // Type into no particular element to activate input

            String str = value.toString();
            WebElement inputCell = elementCache().inputCell();
            inputCell.sendKeys(Keys.BACK_SPACE, str, Keys.RETURN); // Add the RETURN to close the inputCell.

            getWrapper().shortWait().until(ExpectedConditions.stalenessOf(inputCell));

            if(checkContains)
            {
                // Wait until the grid cell has the updated text. Check for contains, not equal, because when updating a cell
                // the cell's new value will be the old value plus the new value and the cursor may not be placed at the end
                // of the existing value so the new value should exist somewhere in the cell text value not necessarily
                // at the end of it.
                WebDriverWrapper.waitFor(() -> gridCell.getText().contains(str),
                        "Value entered into inputCell '" + value + "' did not appear in grid cell.", WAIT_FOR_JAVASCRIPT);
            }
            else
            {
                // Wait until the grid cell is not the same as before.
                WebDriverWrapper.waitFor(() -> !gridCell.getText().equals(beforeText),
                        "Value entered into inputCell '" + value + "' did not appear in grid cell.", WAIT_FOR_JAVASCRIPT);
            }
        }
        return gridCell;
    }

    /**
     * Creates a value in a select that allows the user to insert/create a value, vs. selecting from an existing/populated set
     * @param row   the row
     * @param columnName    name of the column
     * @param value     value to insert
     */
    public void setNewSelectValue(int row, String columnName, String value)
    {
        WebElement gridCell = selectCell(row, columnName);

        ReactSelect createSelect = elementCache().lookupSelect(gridCell);

        createSelect.createValue(value);
    }

    /**
     * Search for a row and then clear the given cell (columnNameToClear) on the row.
     *
     * @param columnNameToSearch Column to search.
     * @param valueToSearch Value in the column to search for.
     * @param columnNameToClear Column to clear.
     */
    public void clearCellValue(String columnNameToSearch, String valueToSearch, String columnNameToClear)
    {
        clearCellValue(getRowIndex(columnNameToSearch, valueToSearch), columnNameToClear);
    }


    /**
     * Clear the cell (columnName) in the row.
     *
     * @param row Row of the cell to clear.
     * @param columnName Column of the cell to clear.
     */
    public void clearCellValue(int row, String columnName)
    {
        selectCell(row, columnName);
        new Actions(getDriver()).sendKeys(Keys.DELETE).perform();
    }

    /**
     * For a given row get the value in the given column.
     *
     * @param row The row index (0 based).
     * @param columnName The name of the column to get the value for.
     * @return The string value of the {@link WebElement} that is the cell.
     */
    public String getCellValue(int row, String columnName)
    {
        return getCellValue(getCell(row, columnName));
    }

    private String getCellValue(WebElement cell)
    {
        return cell.getText().trim();
    }


    /**
     * Dismiss the dropdown list that is currently displayed on the grid.
     *
     * @return A reference to this EditableGrid.
     */
    public EditableGrid dismissDropdownList()
    {
        ReactSelect.finder(getDriver()).find(getComponentElement()).close();

        return this;
    }

    /**
     * For the given row get the values displayed in the dropdown list for the given column.
     *
     * @param row The 0 based row index.
     * @param columnName The name of the column.
     * @return A list of strings from the dropdown list. If the cell does not have a dropdown then an empty list is returned.
     */
    public List<String> getDropdownListForCell(int row, String columnName)
    {
        return getFilteredDropdownListForCell(row, columnName, null);
    }

    /**
     * For the given row and column type some text into the cell to get the 'filtered' values displayed in the dropdown list.
     * If this cell is not a lookup cell, does not have a dropdown, the text will not be entered and an empty list will be returned.
     *
     * @param row A 0 based index containing the cell.
     * @param columnName The column of the cell.
     * @param filterText The text to type into the cell. If the value is null it will not filter the list.
     * @return A list values shown in the dropdown list after the text has been entered.
     */
    public List<String> getFilteredDropdownListForCell(int row, String columnName, @Nullable String filterText)
    {

        WebElement gridCell = selectCell(row, columnName);

        ReactSelect lookupSelect = elementCache().lookupSelect(gridCell);

        // If the click did not expand the select this will.
        // This will have no effect if the list is expended.
        lookupSelect.open();

        if (StringUtils.isNotBlank(filterText))
        {
            lookupSelect.enterValueInTextbox(filterText);
        }

        return lookupSelect.getOptions();
    }


    /**
     * Pastes delimited text to the grid, via a single target.  The component is clever enough to target
     * text into cells based on text delimiters; thus we can paste a square of data into the grid.
     * @param row           index of the target cell
     * @param columnName    column of the target cell
     * @param pasteText     tab-delimited or csv or excel data
     * @return A Reference to this editableGrid object.
     */
    public EditableGrid pasteFromCell(int row, String columnName, String pasteText)
    {
        return pasteFromCell(row, columnName, pasteText, false);
    }

    /**
     * Pastes delimited text to the grid, via a single target.  The component is clever enough to target
     * text into cells based on text delimiters; thus we can paste a square of data into the grid.
     * @param row           index of the target cell
     * @param columnName    column of the target cell
     * @param pasteText     tab-delimited or csv or excel data
     * @param validate      whether to await/confirm the presence of pasted text before resuming
     * @return A Reference to this editableGrid object.
     */
    public EditableGrid pasteFromCell(int row, String columnName, String pasteText, boolean validate)
    {
        int initialRowCount = getRowCount();
        WebElement gridCell = getCell(row, columnName);
        String indexValue = gridCell.getText();
        selectCell(gridCell);

        getWrapper().actionPaste(null, pasteText);

        // wait for the cell value to change or the rowcount to change, and the target cell to go into highlight,
        // ... or for a second and a half
        WebDriverWrapper.waitFor(()-> (getRowCount() > initialRowCount || !indexValue.equals(gridCell.getText())) &&
                        isInSelection(gridCell), 1500);
        if (validate)
            waitForAnyPasteContent(pasteText);

        return this;
    }

    /**
     * Awaits any elements (except for empty, or space-only) of the pasted content to be present in the grid
     * @param pasteContent  tab-separated text
     */
    protected void waitForAnyPasteContent(String pasteContent)
    {
        // split pasteContent into its parts
        var contentParts = pasteContent.replace("\n", "\t").split("\t");
        // filter out empty and space-only values
        var filteredParts = Arrays.stream(contentParts).filter(a-> !a.isEmpty() && !a.equals(" ")).collect(Collectors.toList());
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(()-> Assertions.assertThat(getSelectionCellTexts())
                        .containsAnyElementsOf(filteredParts));
    }

    /**
     * Awaits all elements (except empty or space-only) of the pasted content to be present in the grid.
     * Use this to validate all expected content appears after pasting to the grid
     * @param pasteContent  tab-separated text of the sort usually pasted into the edit grid
     */
    public void waitForPasteContent(String pasteContent)
    {
        // split pasteContent into its parts
        var contentParts = pasteContent.replace("\n", "\t").split("\t");
        // filter out empty and space-only values
        var filteredParts = Arrays.stream(contentParts).filter(a-> !a.isEmpty() && !a.equals(" ")).collect(Collectors.toList());
        await().atMost(Duration.ofSeconds(2))
                .untilAsserted(()-> Assertions.assertThat(getSelectionCellTexts())
                        .containsAll(filteredParts));
    }

    // captures the texts of any cells currently in selection
    public List<String> getSelectionCellTexts()
    {
        var cells = Locator.tagWithClass("div", "cellular-display")
                .withAttributeContaining("class","cell-selection").findElements(this);
        return getWrapper().getTexts(cells);
    }

    /**
     * Pastes a single value into as many cells as are selected, or supports pasting a square shaped blob of data
     * of the same shape as the prescribed selection.  If a single value is supplied, that value will be put into
     * every cell in the selection.  If the data doesn't match the selection dimensions (e.g., has fewer or more columns)
     * the grid should produce an error/alert.
     * @param pasteText     The text to paste
     * @param startRowIndex index of the starting row
     * @param startColumn   text of the starting cell
     * @param endRowIndex   index of the ending row
     * @param endColumn     text of the ending cell
     * @return  the current grid instance
     */
    public EditableGrid pasteMultipleCells(String pasteText, int startRowIndex, String startColumn, int endRowIndex, String endColumn)
    {
        WebElement startCell = getCell(startRowIndex, startColumn);
        WebElement endCell = getCell(endRowIndex, endColumn);
        selectCellRange(startCell, endCell);
        getWrapper().actionPaste(null, pasteText);
        return this;
    }

    /**
     * Copies text from the grid, b
     * @param startRowIndex Index of the top-left cell's row
     * @param startColumn   Column header of the top-left cell
     * @param endRowIndex   Index of the bottom-right cell's row
     * @param endColumn     Column header of the bottom-right cell
     * @return  the text contained in the prescribed selection
     */
    public String copyCellRange(int startRowIndex, String startColumn, int endRowIndex, String endColumn) throws IOException, UnsupportedFlavorException
    {
        WebElement startCell = getCell(startRowIndex, startColumn);
        WebElement endCell = getCell(endRowIndex, endColumn);
        selectCellRange(startCell, endCell);
        return copyCurrentSelection();
    }

    /**
     * Selects all cells in the table, then copies their contents into delimited text
     * @return  delimited text content of the cells in the grid
     */
    public String copyAllCells() throws IOException, UnsupportedFlavorException
    {
        selectAllCells();
        WebDriverWrapper.waitFor(this::areAllInSelection,
                "expect all cells to be selected before copying grid values", 1500);

        String selection = copyCurrentSelection();
        if (selection.isEmpty())
        {
            log("initial attempt to copy current selection came up empty.  re-trying after 3000 ms");
            new WebDriverWait(getDriver(), Duration.ofSeconds(3));
            return copyCurrentSelection();
        }
        return selection;
    }

    /**
     * Selects all cells in the table, then deletes their content
     */
    public void clearAllCells()
    {
        selectAllCells();
        new Actions(getDriver()).sendKeys(Keys.DELETE).perform();
    }

    public String copyCurrentSelection() throws IOException, UnsupportedFlavorException
    {
        // now copy the contents of the current selection to the clipboard
        Keys cmdKey = MODIFIER_KEY;
        Actions actions = new Actions(getDriver());
        actions.keyDown(cmdKey)
                .sendKeys( "c")
                .keyUp(cmdKey)
                .build()
                .perform();

        return  (String) Toolkit.getDefaultToolkit().getSystemClipboard()
                .getData(DataFlavor.stringFlavor);
    }

    public void dragFill(WebElement startCell, WebElement endCell)
    {
        Locator.XPathLocator selectionHandleLoc = Locator.byClass("cell-selection-handle");
        WebElement selectionHandle = selectionHandleLoc.findElement(startCell);
        dragToCell(selectionHandle, endCell);
        selectionHandleLoc.waitForElement(endCell, 5_000);
    }

    public void selectCellRange(WebElement startCell, WebElement endCell)
    {
        dragToCell(startCell, endCell);

        WebDriverWrapper.waitFor(()-> isInSelection(startCell) && isInSelection(endCell),
                "Cell range did not become selected", 2000);
    }

    private void dragToCell(WebElement elementToDrag, WebElement destinationCell)
    {
        var size = destinationCell.getSize();

        new Actions(getDriver())
                // WebDriver doesn't calculate correct location to click the cell selection handle
                .moveToElement(elementToDrag, 0, 7)
                .clickAndHold()
                .moveToElement(destinationCell)
                // Extra wiggle to get it to stick
                .moveByOffset(0, -size.getHeight())
                .moveByOffset(0, size.getHeight())
                .release()
                .perform();
    }

    private void selectAllCells()
    {
        if (!areAllInSelection())
        {
            int indexOffset = hasSelectColumn() ? 1 : 0;
            selectCell(getCell(0, getColumnNames().get(1 + indexOffset)));    // forces the index cell into selected state
                                                                // this resets the grid state to a known base condition
            // use 'ctrl-a' to select the entire grid
            Keys cmdKey = MODIFIER_KEY;
            new Actions(getDriver()).keyDown(cmdKey).sendKeys("a").keyUp(cmdKey).build().perform();
            WebDriverWrapper.waitFor(this::areAllInSelection,
                    "the expected cells did not become selected", 3000);
        }
    }

    public WebElement selectCell(int row, String columnName)
    {
        // Get a reference to the cell.
        WebElement gridCell = getCell(row, columnName);

        // Select the cell.
        selectCell(gridCell);
        return gridCell;
    }

    /**
     * puts the specified cell into a selected state (appears as a dark-blue outline) with an active input present in it.
     */
    private void selectCell(WebElement cell)
    {
        getWrapper().scrollIntoView(cell);

        if (!isCellSelected(cell))
        {
            cell.click();
            WebDriverWrapper.waitFor(()->  isCellSelected(cell),
                    "the target cell did not become selected", 4000);
        }
    }

    private void activateCell(WebElement cell)
    {
        // If it is a selector, and it already has focus (is active), it will not have a div.cellular-display
        if (Locator.tagWithClass("div", "select-input__control--is-focused").findElements(cell).isEmpty())
            sendKeysToCell(cell, Keys.ENTER);
    }

    public void sendKeysToCell(WebElement cell, CharSequence... keysToSend)
    {
        var cellContent = Locator.tagWithClass("div", "cellular-display").findElement(cell);
        cellContent.sendKeys(keysToSend);
    }

    /**
     * Tests the specified webElement to see if it is in 'cell-selected' state, which means it has an active/focused input in it
     * @param cell A WebElement that is the grid cell (a  td).
     * @return True if the edit is present
     */
    public boolean isCellSelected(WebElement cell)
    {
        try
        {
            // If the cell is a reactSelect, and it is open/active, this will throw a NoSuchElementException because the
            // div will not have the cell-selected in the class attribute.
            return Locator.tagWithClass("div", "cellular-display")
                    .findElement(cell)
                    .getAttribute("class").contains("cell-selected");
        }
        catch (NoSuchElementException nse)
        {
            // If the cell is an open/active reactSelect the class attribute is different.
            return Locator.tagWithClass("div", "select-input__control")
                    .findElement(cell)
                    .getAttribute("class").contains("select-input__control--is-focused");
        }
    }

    /**
     *  tests the specified cell element to see if it is highlighted as a single-or-multi-cell selection area.  this appears as
     *  light-blue background, and is distinct from 'selected'
     * @param cell WebElement (grid td) to check.
     */
    private boolean isInSelection(WebElement cell)  // 'in selection' shows as blue color, means it is part of one or many selected cells for copy/paste, etc
    {
        // Should not need to add code for a reactSelect here. A selection involves clicking/dragging, which closes the reactSelect.
        return Locator.tagWithClass("div", "cellular-display")
                .findElement(cell)
                .getAttribute("class").contains("cell-selection");
    }

    /**
     * attempts to determine whether the entire grid is selected
     * assumes that the first row is never selectable (it's either a selector row, or a row-number cell)
     * @return  True if the top-left and bottom-right cells are 'in-selection', otherwise false
     */
    private boolean areAllInSelection()
    {
        List<String> columns = getColumnNames();
        int selectIndexOffset = hasSelectColumn() ? 1 : 0;
        WebElement indexCell = getCell(0, columns.get(1 + selectIndexOffset));
        WebElement endCell = getCell(getRows().size()-1, columns.get(columns.size()-1));
        return (isInSelection(indexCell) && isInSelection(endCell));
    }

    private boolean cellHasWarning(WebElement cell)
    {
        return Locator.tagWithClass("div", "cell-warning").existsIn(cell);
    }

    public String getCellError(int row, String column)
    {
        WebElement gridCell = getCell(row, column);

        if (! cellHasWarning(gridCell))
            return null;
        else
            return Locator.tagWithClass("div", "cell-warning").findElement(gridCell).getText();
    }

    public String getCellPopoverText(int row, String column)
    {
        WebElement warnDiv = Locator.tagWithClass("div", "cellular-display").findElement(getCell(row, column));
        getWrapper().mouseOver(warnDiv);   // cause the tooltip to be present
        if (WebDriverWrapper.waitFor(()-> null != warnDiv.getAttribute("aria-describedby"), 1000))
        {
            String popoverId = warnDiv.getAttribute("aria-describedby");
            return Locator.id(popoverId).findElement(getDriver()).getText();
        }
        else
            return null;
    }

    public List<WebElement> getCellErrors()
    {
        return Locator.tagWithClass("div", "cell-warning").findElements(this);
    }

    public boolean isDisplayed()
    {
        try
        {
            return getComponentElement().isDisplayed();
        }
        catch (NoSuchElementException nse)
        {
            return false;
        }
    }

    public void setAddRows(int count)
    {
        elementCache().addCountInput.set(String.valueOf(count));
    }

    public void addRows(int count)
    {
        setAddRows(count);
        doAndWaitForUpdate(() -> {
            elementCache().addRowsButton.click();
        });
    }

    private void doAndWaitForUpdate(Runnable func)
    {
        int initialCount = getRowCount();

        func.run();

        waitFor(() -> getRowCount() != initialCount, "Failed to add/remove rows", 5_000);
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        final WebElement topControls = Locator.byClass("editable-grid-buttons__action-buttons").findWhenNeeded(this);

        final WebElement bulkInsertBtn = Locator.byClass("bulk-add-button").findWhenNeeded(topControls);
        final WebElement bulkUpdateBtn = Locator.byClass("bulk-update-button").findWhenNeeded(topControls);
        final WebElement deleteRowsBtn =  Locator.byClass("bulk-remove-button").findWhenNeeded(topControls);
        final ExportMenu exportMenu = ExportMenu.finder(getDriver()).findWhenNeeded(topControls);

        final WebElement table = Locator.byClass("table-cellular").findWhenNeeded(this);

        private final WebElement selectColumn = Locator.xpath("//th/input[@type='checkbox']").findWhenNeeded(table);

        private List<String> columnNames = new ArrayList<>();

        public List<String> getColumnNames()
        {

            // If the number of header cells is not equal to the list of columnName the columns have been modified since
            // the last call to getColumnNames so get the column names again.
            List<WebElement> headerCells = Locators.headerCells.waitForElements(table, WAIT_FOR_JAVASCRIPT);
            if(columnNames.size() != headerCells.size())
            {
                columnNames = new ArrayList<>();
            }

            if (columnNames.isEmpty())
            {

                for (WebElement el : headerCells)
                {
                    columnNames.add(el.getText().trim());
                }

                int rowNumberColumn;

                if (hasSelectColumn())
                {
                    columnNames.set(0, SELECT_COLUMN_HEADER);
                    rowNumberColumn = 1;
                }
                else
                {
                    rowNumberColumn = 0;
                }

                if (columnNames.get(rowNumberColumn).trim().isEmpty())
                {
                    columnNames.set(rowNumberColumn, ROW_NUMBER_COLUMN_HEADER);
                }
            }

            return columnNames;
        }


        public WebElement inputCell()
        {
            return Locators.inputCell.findElement(table);
        }

        public ReactSelect lookupSelect(WebElement cell)
        {
            Locator.byClass("cell-menu-selector").findOptionalElement(cell).ifPresent(WebElement::click);
            ReactSelect lookupSelect = ReactSelect.finder(getDriver()).timeout(2_000).find(table);
            waitFor(()->lookupSelect.isInteractive() && !lookupSelect.isLoading(), "Select control is not ready.", 1_000);
            return lookupSelect;
        }

        public ReactDateTimePicker datePicker()
        {
            return new ReactDateTimePicker.ReactDateTimeInputFinder(getDriver()).withClassName("date-input-cell").find(table);
        }

        final WebElement addRowsPanel = Locator.byClass("editable-grid__controls").findWhenNeeded(this);
        final Input addCountInput = Input.Input(Locator.name("addCount"), getDriver()).findWhenNeeded(addRowsPanel);
        final WebElement addRowsButton = Locator.byClass("btn-primary").findWhenNeeded(addRowsPanel);
    }

    protected abstract static class Locators
    {
        private Locators()
        {
            // Do nothing constructor to prevent instantiation.
        }

        static final Locator loadingGrid = Locator.css("tbody tr.grid-loading");
        static final Locator emptyGrid = Locator.css("tbody tr.grid-empty");
        static final Locator spinner = Locator.css(".fa-spinner");
        static final Locator.XPathLocator rows = Locator.tag("tbody").childTag("tr").withoutClass("grid-empty").withoutClass("grid-loading");
        static final Locator headerCells = Locator.css("thead tr th");
        static final Locator inputCell = Locator.tagWithClass("input", "cellular-input");

    }

    public static class EditableGridFinder extends WebDriverComponent.WebDriverComponentFinder<EditableGrid, EditableGridFinder>
    {
        private final Locator _locator = Locator.byClass("editable-grid__container").parent();

        public EditableGridFinder(WebDriver driver)
        {
            super(driver);
        }

        @Override
        protected EditableGrid construct(WebElement el, WebDriver driver)
        {
            return new EditableGrid(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }
}
