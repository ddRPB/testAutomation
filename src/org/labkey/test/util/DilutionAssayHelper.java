/*
 * Copyright (c) 2014 LabKey Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.labkey.test.util;

import org.jetbrains.annotations.Nullable;
import org.labkey.test.BaseWebDriverTest;
import org.labkey.test.Locator;
import org.labkey.test.components.html.BootstrapMenu;

/**
 * Created by klum on 3/3/14.
 */
public class DilutionAssayHelper
{
    private BaseWebDriverTest _test;

    public DilutionAssayHelper(BaseWebDriverTest test)
    {
        _test = test;
    }

    public void verifyDataIdentifiers(AssayImportOptions.VisitResolverType type, String ptidSuffix)
    {
        _test.log("Verifying data identifiers");

        // verify menu items
        switch (type)
        {
            case ParticipantDate:
                openDataIdentifierMenu();

                _test.waitForElement(BootstrapMenu.Locators.menuItemDisabled("Specimen ID"));
                _test.waitForElement(BootstrapMenu.Locators.menuItemDisabled("Participant ID / Visit"));
                _test.waitForElement(BootstrapMenu.Locators.menuItem("Participant ID / Date"));
                _test.waitForElement(BootstrapMenu.Locators.menuItemDisabled("Specimen ID / Participant ID / Visit"));
                _test.click(BootstrapMenu.Locators.menuItem("Participant ID / Date"));
                verifyDataIdentifierText(type, ptidSuffix);
                break;
            case ParticipantVisit:
                openDataIdentifierMenu();

                _test.waitForElement(BootstrapMenu.Locators.menuItemDisabled("Specimen ID"));
                _test.waitForElement(BootstrapMenu.Locators.menuItem("Participant ID / Visit"));
                _test.waitForElement(BootstrapMenu.Locators.menuItemDisabled("Participant ID / Date"));
                _test.waitForElement(BootstrapMenu.Locators.menuItemDisabled("Specimen ID / Participant ID / Visit"));
                _test.click(BootstrapMenu.Locators.menuItem("Participant ID / Visit"));
                verifyDataIdentifierText(type, ptidSuffix);
                break;
            case ParticipantVisitDate:
                openDataIdentifierMenu();

                _test.waitForElement(BootstrapMenu.Locators.menuItemDisabled("Specimen ID"));
                _test.waitForElement(BootstrapMenu.Locators.menuItem("Participant ID / Visit"));
                _test.waitForElement(BootstrapMenu.Locators.menuItem("Participant ID / Date"));
                _test.waitForElement(BootstrapMenu.Locators.menuItemDisabled("Specimen ID / Participant ID / Visit"));

                // click and verify the identifiers on the page
                _test.click(BootstrapMenu.Locators.menuItem("Participant ID / Date"));
                verifyDataIdentifierText(AssayImportOptions.VisitResolverType.ParticipantDate, ptidSuffix);
                break;
            case SpecimenIDParticipantVisit:
                openDataIdentifierMenu();

                _test.waitForElement(BootstrapMenu.Locators.menuItem("Specimen ID"));
                _test.waitForElement(BootstrapMenu.Locators.menuItem("Participant ID / Visit"));
                _test.waitForElement(BootstrapMenu.Locators.menuItemDisabled("Participant ID / Date"));
                _test.waitForElement(BootstrapMenu.Locators.menuItem("Specimen ID / Participant ID / Visit"));

                // click and verify the identifiers on the page
                _test.click(BootstrapMenu.Locators.menuItem("Specimen ID / Participant ID / Visit"));
                verifyDataIdentifierText(AssayImportOptions.VisitResolverType.SpecimenIDParticipantVisit, ptidSuffix);
                break;
        }
    }

    /**
     * Renders the data identifier menu, so we can validate the menu items
     */
    private void openDataIdentifierMenu()
    {
        new BootstrapMenu(_test.getDriver(), detailMenu("Change Graph Options")
                .waitForElement(_test.getDriver(), _test.WAIT_FOR_JAVASCRIPT))
                .clickMenuButton(false, true, "Data Identifiers", "Data Identifiers");
    }

    private void verifyDataIdentifierText(AssayImportOptions.VisitResolverType type, @Nullable String ptidSuffix)
    {
        String format = "";

        switch (type)
        {
            case ParticipantDate:
                format = (ptidSuffix == null) ? "ptid %1$d, 2014-02-28" : "ptid %1$d %2$s, 2014-02-28";
                break;
            case ParticipantVisit:
                format = (ptidSuffix == null) ? "ptid %1$d, Vst %3$.1f" : "ptid %1$d %2$s, Vst %3$.1f";
                break;
            case ParticipantVisitDate:
                format = (ptidSuffix == null) ? "ptid %1$d, 2014-02-28" : "ptid %1$d %2$s, 2014-02-28";
                break;
            case SpecimenIDParticipantVisit:
                format = (ptidSuffix == null) ? "SPECIMEN-%1$d, ptid %1$d, Vst %3$.1f" : "SPECIMEN-%1$d, ptid %1$d %2$s, Vst %3$.1f";
                break;
        }

        for (int i=1; i < 6; i++)
        {
            String text = String.format(format, i, ptidSuffix, (double)i);

            // cutoff
            _test.waitForElement(Locator.xpath("//table").withClass("cutoff-table").append("//td").withClass("sample-heading").withText(text));

            // dilution
            _test.assertElementPresent(Locator.xpath("//table").withClass("labkey-data-region").append("//td").withClass("labkey-data-region-header-container").withText(text));
        }
    }

    public void clickDetailsLink(String text, String ... subMenuLabels)
    {
        if (subMenuLabels == null || subMenuLabels.length == 0)
        {
            _test.clickAndWait(Locator.linkWithText(text));
        }
        else
        {
            new BootstrapMenu(_test.getDriver(), detailMenu(text).waitForElement(_test.getDriver(), _test.WAIT_FOR_JAVASCRIPT))
                    .clickMenuButton(true, false, subMenuLabels);
        }
    }

    private Locator.XPathLocator detailMenu(String text)
    {
        return Locator.tagWithClassContaining("div", "lk-menu-drop")
                .withChild(Locator.tagWithAttribute("a", "data-toggle", "dropdown").withText(text));
    }
}
