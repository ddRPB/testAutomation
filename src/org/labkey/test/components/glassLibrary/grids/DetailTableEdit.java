package org.labkey.test.components.glassLibrary.grids;

import org.labkey.test.Locator;
import org.labkey.test.components.WebDriverComponent;
import org.labkey.test.components.glassLibrary.components.ReactSelect;
import org.labkey.test.components.html.Checkbox;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.NoSuchElementException;

import java.util.Arrays;
import java.util.List;

public class DetailTableEdit extends WebDriverComponent
{

    private final WebElement _editPanel;
    private final WebDriver _driver;

    protected DetailTableEdit(WebElement editPanel, WebDriver driver)
    {
        _editPanel = editPanel;
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
        return _editPanel;
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
        WebElement fieldValueElement = Locators.fieldValue(fieldCaption).findElement(getComponentElement());
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
        WebElement fieldValueElement = Locators.fieldValue(fieldCaption).findElement(getComponentElement());
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
        WebElement fieldValueElement = Locators.fieldValue(fieldCaption).findElement(getComponentElement());
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

            WebElement fieldValueElement = Locators.fieldValue(fieldCaption).findElement(getComponentElement());

            WebElement editableElement = fieldValueElement.findElement(By.xpath("./div/div/*"));
            String elementType = editableElement.getTagName().toLowerCase().trim();

            switch(elementType)
            {
                case "textarea":
                case "input":
                    editableElement.clear();
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

    /**
     * Get the value of a boolean field.
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return The value of the field.
     **/
    public boolean getBooleanField(String fieldCaption)
    {
        return new Checkbox(Locator.tagWithName("input", fieldCaption.toLowerCase()).findElement(getComponentElement())).isChecked();
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

        if(isFieldEditable(fieldCaption))
        {

            WebElement fieldValueElement = Locators.fieldValue(fieldCaption).findElement(getComponentElement());

            WebElement editableElement = fieldValueElement.findElement(Locator.tagWithName("input", fieldCaption.toLowerCase()));
            String elementType = editableElement.getAttribute("type").toLowerCase().trim();

            if(elementType.equals("checkbox"))
            {
                Checkbox checkbox = new Checkbox(editableElement);

                if(checkbox.isChecked() && !value)
                {
                    // If value is false and the checkbox is checked, uncheck it.
                    checkbox.check();
                }
                else if(!checkbox.isChecked() && value)
                {
                    // If value is true and the checkbox is not checked, check it.
                    checkbox.check();
                }

            }
            else
            {
                throw new NoSuchElementException("Field '" + fieldCaption + "' is not a checkbox, cannot be set to true/false.");
            }
        }
        else
        {
            throw new IllegalArgumentException("Field with caption '" + fieldCaption + "' is read-only. This field can not be set.");
        }

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

    /**
     * Get the value of an select field. This could be one or many values, because of this the result is returned as a list.
     *
     * @param fieldCaption The caption/label of the field to get.
     * @return A (String) list of the values selected.
     **/
    public List<String> getSelectValue(String fieldCaption)
    {
        return new ReactSelect.ReactSelectFinder(_driver).followingLabelWithSpan(fieldCaption).find().getSelections();
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

    /**
     * Select multiple values from a select list.
     *
     * @param fieldCaption The caption/label of the field to set.
     * @param selectValues The value to select from the list.
     * @return A reference to this editable detail table.
     **/
    public DetailTableEdit setSelectValue(String fieldCaption, List<String> selectValues)
    {
        ReactSelect reactSelect = new ReactSelect.ReactSelectFinder(_driver).followingLabelWithSpan(fieldCaption).find();
        selectValues.forEach(s -> {reactSelect.select(s);});
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
        new ReactSelect.ReactSelectFinder(_driver).followingLabelWithSpan(fieldCaption).find().clearSelection();
        return this;
    }

    protected static abstract class Locators
    {

        static Locator fieldValue(String caption)
        {
            return Locator.tagWithAttribute("td", "data-caption", caption);
        }

    }

    public static class DetailTableEditFinder extends WebDriverComponent.WebDriverComponentFinder<DetailTableEdit, DetailTableEditFinder>
    {
        private Locator _locator;

        public DetailTableEditFinder(WebDriver driver)
        {
            super(driver);
            _locator= Locator.tagWithClass("div", "detail__editing");
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