/*
 * Copyright (c) 2013 LabKey Corporation
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

public class ModulePropertyValue
{
    private String _moduleName;
    private String _containerPath;
    private String _propertyName;
    private String _value;

    public ModulePropertyValue(String moduleName, String containerPath, String propertyName, String value)
    {
        _moduleName = moduleName;
        _containerPath = containerPath;
        _propertyName = propertyName;
        _value = value;
    }

    public String getModuleName()
    {
        return _moduleName;
    }

    public String getContainerPath()
    {
        return _containerPath;
    }

    public String getPropertyName()
    {
        return _propertyName;
    }

    public String getValue()
    {
        return _value;
    }
}
