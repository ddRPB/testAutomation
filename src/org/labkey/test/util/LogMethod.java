/*
 * Copyright (c) 2012-2013 LabKey Corporation
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * User: tchadick
 * Date: 10/1/12
 * Time: 2:30 PM
 */

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface LogMethod
{
    // Should be used to mark methods that we want to log the execution time for
    // but are not complex enough to warrent logging both their entrance and exit
    // Such marked methods will only be logged upon returning
    boolean quiet() default false;

    MethodType category() default MethodType.UNSPECIFIED;

    public static enum MethodType
    {
        BEFORE,
        SETUP,
        VERIFICATION,
        MIXEDPURPOSE,
        UNSPECIFIED,
        AFTER
    }
}
