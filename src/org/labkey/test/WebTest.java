/*
 * Copyright (c) 2007-2011 LabKey Corporation
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

package org.labkey.test;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Mark Igra
 * Date: Feb 8, 2007
 * Time: 4:26:59 PM
 */
public interface WebTest
{
    String getResponseText();
    int getResponseCode();
    void beginAt(String url);
    void log(String str);
    URL getURL() throws MalformedURLException;
    String[] getLinkAddresses();
    List<String> getCreatedProjects();
    String getAssociatedModuleDirectory();
}
