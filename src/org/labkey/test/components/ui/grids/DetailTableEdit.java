package org.labkey.test.components.ui.grids;

import org.junit.Assert;
import org.labkey.test.BootstrapLocators;
import org.labkey.test.Locator;
import org.labkey.test.WebDriverWrapper;
import org.labkey.test.components.Component;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.html.Checkbox;
import org.labkey.test.components.html.Input;
import org.labkey.test.components.react.FilteringReactSelect;
import org.labkey.test.components.react.ReactDateTimePicker;
import org.labkey.test.components.react.ReactSelect;
import org.labkey.test.components.ui.files.FileUploadField;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * automates /QueryModel/DetailPanel.tsx in its editable mode
 */
public class DetailTableEdit extends WebDriverComponent<DetailTableEdit.ElementCache>
{
    private final WebElement _formElement;
    private final WebDriver _driver;
    private String _title;
    private int _readyTimeout = WebDriverWrapper.WAIT_FOR_JAVASCRIPT;

    protected DetailTableEdit(WebElement formElement, WebDriver driver)
    {
        _formElement = formElement;
        _driver = driver;
    }

    @Override
    protected WebDriver getDriver()
    {
        return _driver;
    }

    @Override
    public WebElement getComponentElement()
    {
        return _formElement;
    }

    public String getTitle()
    {
        if (_title == null)
            _title = elementCache().header.getText();
        return _title;
    }

    public DetailTableEdit setReadyTimeout(int readyTimeout)
    {
        _readyTimeout = readyTimeout;
        return this;
    }

    /**
     * Check to see if a field is editable. Could be state dependent, that is it returns false if the field is
     * loading but if checked later could return true.
     *
     * @param fieldCaption The caption/label of the field to check.
     * @return True if it is false otherwise.
     **/
    public boolean isFieldEditable(String fieldCaption)
    {
        // TODO Could put a check here to see if a field is loading then return false, or wait.
        WebElement fieldValueElement = elementCache().fieldValue(fieldCaption);
        return isEditableField(fieldValueElement);
    }

    private boolean isEditableField(WebElement element)
    {
        // If the div does not have the class value of 'field__un-editable' then it is an editable field.
        return Locator.css("div:not(.field__un-editable)").findOptionalElement(element).isPresent();
    }

    /**
     * Get the value of a read only field.
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return The value in the field.
     **/
    public String getReadOnlyField(String fieldCaption)
    {
        WebElement fieldValueElement = elementCache().fieldValue(fieldCaption);
        return fieldValueElement.findElement(By.xpath("./div/*")).getText();
    }

    /**
     * Get the value of a text field.
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return The value in the field.
     **/
    public String getTextField(String fieldCaption)
    {
        WebElement fieldValueElement = elementCache().fieldValue(fieldCaption);
        WebElement textElement = fieldValueElement.findElement(By.xpath("./div/div/*"));
        if(textElement.getTagName().equalsIgnoreCase("textarea"))
            return textElement.getText();
        else
            return textElement.getAttribute("value");
    }

    /**
     * Set a text field.
     *
     * @param fieldCaption The caption/label of the field to set.
     * @param value The value to set the field to.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setTextField(String fieldCaption, String value)
    {
        if(isFieldEditable(fieldCaption))
        {
            WebElement fieldValueElement = elementCache().fieldValue(fieldCaption);

            WebElement editableElement = fieldValueElement.findElement(By.xpath("./div/div/*"));
            String elementType = editableElement.getTagName().toLowerCase().trim();

            switch(elementType)
            {
                case "textarea":
                case "input":
                    editableElement.clear();
                    WebDriverWrapper.waitFor(()->editableElement.getText().isEmpty(), 500);
                    editableElement.sendKeys(value);
                    break;
                default:
                    throw new NoSuchElementException("This doesn't look like an 'input' or 'textarea' element, are you sure you are calling the correct method?");
            }
        }
        else
        {
            throw new IllegalArgumentException("Field with caption '" + fieldCaption + "' is read-only. This field can not be set.");
        }

        return this;
    }

    public DetailTableEdit setInputByFieldName(String fieldName, String value)
    {
        Locator inputloc = Locator.tagWithClass("input", "form-control")
            .withAttribute("name",  fieldName);
        Input input = Input.Input(inputloc,
                getDriver()).waitFor();
        input.set(value);
        return this;
    }

    public DetailTableEdit setTextareaByFieldName(String fieldName, String value)
    {
        Locator inputloc = Locator.tagWithClass("textarea", "form-control")
                .withAttribute("name",  fieldName);
        Input input = Input.Input(inputloc,
                getDriver()).waitFor();
        input.set(value);
        return this;
    }

    /**
     * Get the value of a boolean field.
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return The value of the field.
     **/
    public boolean getBooleanField(String fieldCaption)
    {
        // The text used in the field caption and the value of the name attribute in the checkbox don't always have the same case.
        WebElement editableElement = Locator.tagWithAttributeIgnoreCase("input", "name", fieldCaption).findElement(getComponentElement());
        String elementType = editableElement.getAttribute("type").toLowerCase().trim();

        Assert.assertEquals(String.format("Field '%s' is not a checkbox. Cannot be get true/false value.", fieldCaption), "checkbox", elementType);

        return new Checkbox(editableElement).isChecked();
    }

    /**
     * Set a boolean field (a checkbox).
     *
     * @param fieldCaption The caption/label of the field to set.
     * @param value True will check it, false will uncheck it.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setBooleanField(String fieldCaption, boolean value)
    {

        WebElement fieldValueElement = elementCache().fieldValue(fieldCaption);
        Assert.assertTrue(String.format("Field '%s' is not editable and cannot be set.", fieldCaption), isEditableField(fieldValueElement));

        // The text used in the field caption and the value of the name attribute in the checkbox don't always have the same case.
        WebElement editableElement = fieldValueElement.findElement(Locator.tagWithAttributeIgnoreCase("input", "name", fieldCaption));
        String elementType = editableElement.getAttribute("type").toLowerCase().trim();

        Assert.assertEquals(String.format("Field '%s' is not a checkbox. Cannot be set to true/false.", fieldCaption), "checkbox", elementType);

        Checkbox checkbox = new Checkbox(editableElement);

        checkbox.set(value);

        return this;
    }

    /**
     * Get the value of an int field. You could also call getTextField
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return The value of the field as an int.
     **/
    public int getIntField(String fieldCaption)
    {
        return Integer.getInteger(getTextField(fieldCaption));
    }

    /**
     * Set an int field.
     *
     * @param fieldCaption The caption/label of the field to set.
     * @param value The int value to set the field to.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setIntField(String fieldCaption, int value)
    {
        return setTextField(fieldCaption, Integer.toString(value));
    }

    public FileUploadField getFileField(String fieldCaption)
    {
        return elementCache().fileField(fieldCaption);
    }

    public DetailTableEdit setFileField(String fieldCaption, File file)
    {
        getFileField(fieldCaption)
                .setFile(file);

        return this;
    }

    public DetailTableEdit removeFileField(String fieldCaption)
    {
        getFileField(fieldCaption).removeFile();

        return this;
    }

    public boolean isFileFieldBlank(String fieldCaption)
    {
        return !getFileField(fieldCaption)
                .hasAttachedFile();
    }

    /**
     * Get the value of a select field.
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return The selected value.
     **/
    public String getSelectedValue(String fieldCaption)
    {
        FilteringReactSelect reactSelect = elementCache().findSelect(fieldCaption);
        return reactSelect.getValue();
    }

    /*
        This allows you to query a given select in the edit panel to see what options it offers
     */
    public List<String> getSelectOptions(String fieldCaption)
    {
        FilteringReactSelect reactSelect = elementCache().findSelect(fieldCaption);
        return reactSelect.getOptions();
    }

    /**
     * Select a single value from a select list.
     *
     * @param fieldCaption The caption/label of the field to set.
     * @param selectValue The value to select from the list.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setSelectValue(String fieldCaption, String selectValue)
    {
        List<String> selection = Arrays.asList(selectValue);
        return setSelectValue(fieldCaption, selection);
    }

    public DetailTableEdit createSelectValue(String fieldCaption, String value)
    {
        WebElement container = Locator.tag("td").withAttribute("data-caption", fieldCaption).findElement(this);
        var select = ReactSelect.finder(getDriver()).waitFor(container);
        select.createValue(value);
        return this;
    }


    /**
     * Select multiple values from a select list.
     *
     * @param fieldCaption The caption/label of the field to set.
     * @param selectValues The value to select from the list.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setSelectValue(String fieldCaption, List<String> selectValues)
    {
        FilteringReactSelect reactSelect = elementCache().findSelect(fieldCaption);
        selectValues.forEach(reactSelect::typeAheadSelect);
        return this;
    }

    /**
     * Clear a given select field.
     *
     * @param fieldCaption The caption/label of the field to clear.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit clearSelectValue(String fieldCaption)
    {
        return clearSelectValue(fieldCaption, true, true);
    }

    /**
     * Clear a given select field
     * @param fieldCaption The caption/label of the field to clear.
     * @param waitForSelection If true, wait for the select to have a selection before clearing it
     * @param assertSelection  If true, assert if no selection appears (note: does nothing if waitForSelection is not true)
     * @return
     */
    public DetailTableEdit clearSelectValue(String fieldCaption, boolean waitForSelection, boolean assertSelection)
    {
        var select = elementCache().findSelect(fieldCaption);
        if (waitForSelection)
        {
            if (assertSelection) {
                WebDriverWrapper.waitFor(() -> select.hasSelection(),
                        String.format("The %s select did not have any selection in time", fieldCaption), _readyTimeout);
            }
            else {
                WebDriverWrapper.waitFor(() -> select.hasSelection(), 1000);
            }
        }
        select.clearSelection();
        return this;
    }

    /**
     * Set a DateTime, Date or Time field.
     * @param fieldCaption The caption of the field to set.
     * @param dateTime Will be used to determine what kind of field is being set and how to set it. If the parameter
     *                 is a LocalDateTime object then it is assumed that field is a DateTime field. If the parameter is
     *                 a LocalDate object then it is assumed to be a date-only field. And I think you can guess what
     *                 happens with a LocalTime object type. If the type is a string it is used as a literal value that
     *                 is typed into the field (no picker is used).
     * @return A reference to this DetailTableEdit object.
     */
    public DetailTableEdit setDateTimeField(String fieldCaption, Object dateTime)
    {
        ReactDateTimePicker dateTimePicker = getDateTimePicker(fieldCaption);
        if(dateTime instanceof LocalDateTime localDateTime)
        {
            dateTimePicker.select(localDateTime);
        }
        else if(dateTime instanceof LocalDate localDate)
        {
            dateTimePicker.selectDate(localDate);
        }
        else if(dateTime instanceof LocalTime localTime)
        {
            dateTimePicker.selectTime(localTime);
        }
        else if(dateTime instanceof String setValue)
        {
            dateTimePicker.set(setValue, true);
        }
        else
        {
            throw new IllegalArgumentException(
                    String.format("Unable to use type %s to set a DateTime, Date or Time field.", dateTime.getClass()));
        }

        return this;
    }

    public String getDateTimeField(String fieldCaption)
    {
        ReactDateTimePicker dateTimePicker = getDateTimePicker(fieldCaption);
        return dateTimePicker.get();
    }

    public void clearDateTimeField(String fieldCaption)
    {
        ReactDateTimePicker dateTimePicker = getDateTimePicker(fieldCaption);
        dateTimePicker.clear();
    }

    private ReactDateTimePicker getDateTimePicker(String fieldCaption)
    {
        return new ReactDateTimePicker.ReactDateTimeInputFinder(getDriver())
                .withInputId(fieldCaption).find(this);
    }

    /**
     * Get the field names shown on the form.
     *
     * @return A list of string with the displayed field names.
     */
    public List<String> getDisplayedFieldNames()
    {
        return Locator.tagWithAttribute("td", "data-fieldkey").findElements(this)
                .stream().map(el -> el.getAttribute("data-caption")).collect(Collectors.toList());
    }

    private String getSourceTitle()
    {
        return getTitle().replace("Editing ", "");
    }

    /**
     * A validation message happens if a value of a particular field is out of bounds or incorrect in some other way.
     *
     * @return The text of the validation message or an empty string if there is none.
     */
    public String getValidationMessage()
    {
        if(elementCache().validationMsg.existsIn(this))
            return elementCache().validationMsg.findElement(getDriver()).getText();
        else
            return "";
    }

    public boolean isSaveButtonEnabled()
    {
        return elementCache().saveButton.isEnabled();
    }

    public DetailDataPanel clickSave()
    {
        String title = getSourceTitle();
        getWrapper().shortWait().until(ExpectedConditions.elementToBeClickable(elementCache().saveButton));
        elementCache().saveButton.click();

        // If save causes some update, wait until it is completed.
        WebDriverWrapper.waitFor(()->!BootstrapLocators.loadingSpinner.existsIn(getDriver()),
                "Save has taken too long to complete.", 15_000);

        return new DetailDataPanel.DetailDataPanelFinder(getDriver()).withTitle(title).waitFor();
    }

    public String clickSaveExpectingError()
    {
        elementCache().saveButton.click();
        WebElement errorBanner = BootstrapLocators.errorBanner.findWhenNeeded(this);
        WebDriverWrapper.waitFor(()->errorBanner.isDisplayed(),
                "No error message was shown.", 1_000);
        return errorBanner.getText();
    }

    public DetailDataPanel clickCancel()
    {
        String title = getSourceTitle();
        elementCache().cancelButton.click();
        return new DetailDataPanel.DetailDataPanelFinder(getDriver()).withTitle(title).waitFor();
    }

    public String clickCancelExpectingError()
    {
        elementCache().cancelButton.click();
        WebElement errorBanner = BootstrapLocators.errorBanner.findWhenNeeded(this);
        WebDriverWrapper.waitFor(()->errorBanner.isDisplayed(),
                "No error message was shown.", 1_000);
        return errorBanner.getText();
    }

    public void setActionComment(String comment)
    {
        elementCache().commentInput.sendKeys(comment);
    }

    public void clearActionComment()
    {
        elementCache().commentInput.clear();
    }

    @Override
    protected ElementCache newElementCache()
    {
        return new ElementCache();
    }

    protected class ElementCache extends Component<?>.ElementCache
    {
        public WebElement header = Locator.tagWithClass("div", "panel-heading")
                .findWhenNeeded(this);
        public WebElement editPanel = Locator.tagWithClass("div", "detail__editing")
                .findWhenNeeded(this);

        public WebElement fieldValue(String caption)
        {
            return Locator.tagWithAttribute("td", "data-caption", caption).findElement(editPanel);
        }

        public FileUploadField fileField(String caption)
        {
            return new FileUploadField(fieldValue(caption), getDriver());
        }

        public Locator validationMsg = Locator.tagWithClass("span", "validation-message");

        public WebElement saveButton = Locator.tagWithAttribute("button", "type", "submit")
                .findWhenNeeded(this);
        public WebElement cancelButton = Locator.tagWithAttribute("button", "type", "button")
                .findWhenNeeded(this);

        public WebElement commentInput = Locator.tagWithId("textarea", "actionComments").refindWhenNeeded(getDriver());

        public FilteringReactSelect findSelect(String fieldCaption)
        {
            WebElement container = Locator.tag("td").withAttribute("data-caption", fieldCaption).findElement(this);
            return FilteringReactSelect.finder(_driver).find(container);
        }
    }

    public static class DetailTableEditFinder extends WebDriverComponent.WebDriverComponentFinder<DetailTableEdit, DetailTableEditFinder>
    {
        private final Locator.XPathLocator _baseLocator = Locator.tag("form")
                .withDescendant(Locator.tagWithClass("table", "detail-component--table__fixed"));
        private Locator _locator;

        public DetailTableEditFinder(WebDriver driver)
        {
            super(driver);
            _locator= _baseLocator;
        }

        public DetailTableEditFinder withTitle(String title)
        {
            _locator = _baseLocator.withDescendant(Locator.tagWithClass("span", "detail__edit--heading")
                .parent().withText(title));
            return this;
        }

        @Override
        protected DetailTableEdit construct(WebElement el, WebDriver driver)
        {
            return new DetailTableEdit(el, driver);
        }

        @Override
        protected Locator locator()
        {
            return _locator;
        }
    }

}
