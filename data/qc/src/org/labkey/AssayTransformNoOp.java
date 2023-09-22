/*
 * Copyright (c) 2015-2019 LabKey Corporation
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
package org.labkey;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class AssayTransformNoOp extends AbstractAssayValidator
{
    public static void main(String[] args)
    {
        if (args.length < 4)
            throw new IllegalArgumentException("Input data file not passed in");

        File runProperties = new File(args[0]);
        if (runProperties.exists())
        {
            AssayTransformNoOp transform = new AssayTransformNoOp();

            transform.runTransform(runProperties, args[1], args[2], args[3]);
        }
        else
            throw new IllegalArgumentException("Input data file does not exist");
    }

    public void runTransform(File inputFile, String username, String password, String host)
    {
        setEmail(username);
        setPassword(password);
        setHost(host);
        parseRunProperties(inputFile);
    }

}