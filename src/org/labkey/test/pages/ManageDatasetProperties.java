/*
 * Copyright (c) 2017 LabKey Corporation
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
package org.labkey.test.pages;

import org.labkey.test.Locator;
import org.openqa.selenium.WebDriver;

/**
 * Created by RyanS on 5/18/2017.
 */

//Probably not needed, may be a dupe of DatasetPropertiesPage

public class ManageDatasetProperties extends LabKeyPage<ManageDatasetProperties.ElementCache>
{
    public ManageDatasetProperties(WebDriver driver)
    {
        super(driver);
    }

    protected class ElementCache extends LabKeyPage.ElementCache
    {
        Locator viewDataBtn = Locator.lkButton("View Data");
        Locator editAssociatedVisitsBtn = Locator.lkButton("Edit Associated Visits");
        Locator manageDatasetsBtn = Locator.lkButton("Manage Datasets");
        Locator deleteDatasetBtn = Locator.lkButton("Delete Dataset");
        Locator deleteAllRowsBtn = Locator.lkButton("Delete All Rows");
    }
}
