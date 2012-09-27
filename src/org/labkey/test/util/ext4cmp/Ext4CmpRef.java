/*
 * Copyright (c) 2012 LabKey Corporation
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
package org.labkey.test.util.ext4cmp;

import org.labkey.test.BaseSeleniumWebTest;
import org.labkey.test.util.Ext4Helper;

import java.util.List;

/**
* Created by IntelliJ IDEA.
* User: markigra
* Date: 5/31/12
* Time: 10:43 PM
* To change this template use File | Settings | File Templates.
*/
public class Ext4CmpRef
{
    protected String _id;
    protected BaseSeleniumWebTest _test;

    public Ext4CmpRef(String id, BaseSeleniumWebTest test)
    {
        this._id = id;
        this._test = test;
    }

    public String getId()
    {
        return _id;
    }

    public List<Ext4CmpRef> query(String selector)
    {
        String res = _test.getWrapper().getEval("selenium.ext4ComponentQuery('" + selector + "', '" + _id + "')");
        return _test._ext4Helper.componentsFromJson(res, Ext4CmpRef.class);
    }

    public String eval(String expr)
    {
        return _test.getWrapper().getEval("selenium.ext4ComponentEval('" + _id + "', '" + expr + "')");
    }
}
