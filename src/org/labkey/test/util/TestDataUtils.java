package org.labkey.test.util;

import org.apache.commons.io.IOUtils;
import org.labkey.serverapi.reader.TabLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TestDataUtils
{
    private TestDataUtils()
    {
        // Utility class. Do not instantiate.
    }

    public static List<Map<String, Object>> rowMapsFromTsv(String tsvString) throws IOException
    {
        try (InputStream dataStream = IOUtils.toInputStream(tsvString, StandardCharsets.UTF_8))
        {
            return new TabLoader.TsvFactory().createLoader(dataStream, true).load();
        }
    }

    public static List<Map<String, Object>> rowMapsFromCsv(String tsvString) throws IOException
    {
        try (InputStream dataStream = IOUtils.toInputStream(tsvString, StandardCharsets.UTF_8))
        {
            return new TabLoader.CsvFactory().createLoader(dataStream, true).load();
        }
    }

    public static String tsvStringFromRowMaps(List<Map<String, Object>> rowMaps, List<String> columns,
                                              boolean includeHeaders)
    {
        return toTabular(rowMaps, columns, "\t", includeHeaders);
    }

    public static String csvStringFromRowMaps(List<Map<String, Object>> rowMaps, List<String> columns,
                                              boolean includeHeaders)
    {
        return toTabular(rowMaps, columns, ",", includeHeaders);
    }


    public static List<List<String>> rowListsFromMaps(List<Map<String, Object>> rowMaps, List<String> columns)
    {
        return rowListsFromMaps(rowMaps, columns, false, true);
    }

    /**
     * convert a List of Map<String, Object> to a list of List<String>
     * @param rowMaps   Source data
     * @param columns   keys contained in each map, will copy values associated with them to the resulting list
     * @return A List<List<String>> containing values
     * @throws IOException
     */
    public static List<List<String>> rowListsFromMaps(List<Map<String, Object>> rowMaps, List<String> columns, boolean includeHeaders, boolean preserveEmptyValues)
    {
        List<List<String>> lists = new ArrayList<>();

        if (includeHeaders)
        {
            List<String> headers = new ArrayList<>();
            for(String col : columns)
                headers.add(col);

            lists.add(headers);
        }

        for (int i=0; i<rowMaps.size(); i++)
        {
            List<String> rowList = new ArrayList<>();
            var rowMap = rowMaps.get(i);
            for(String column : columns)
            {
                var value = (String) rowMap.get(column);
                if (value == null && preserveEmptyValues)
                    rowList.add("");
                else
                    rowList.add(value);
            }
            lists.add(rowList);
        }
        return lists;
    }

    /**
     * Convert a list of Map<String, Object>> to tabluar (tsv, csv) format
     * (assumes the rowMaps all share the same keyset/schema)
     * can be used to generate edit-grid paste data, if delimiter is \t and includeHeaders is false
     *
     * @param rowMaps data to be written into tabular format
     * @param columns the fields (in order) from the rowMaps to include in tabular output
     * @param delimiter comma [,] for csv tab [\t] for tsv
     * @param includeHeaders    whether to write the keys as column names on the first line of the output string
     * @return
     */
    private static String toTabular(List<Map<String, Object>> rowMaps, List<String> columns,
                                    String delimiter, boolean includeHeaders)
    {
        StringBuilder builder = new StringBuilder();

        if (includeHeaders)
        {
            builder.append(String.join(delimiter, columns));
            builder.append("\n");
        }

        for (Map<String, Object> row : rowMaps)
        {
            List<String> values = new ArrayList<>();
            for (String name : columns)
            {
                Object value = row.get(name);
                values.add(value != null ? String.valueOf(value) : "");
            }
            builder.append(String.join(delimiter, values));
            builder.append("\n");
        }
        return builder.toString();
    }
}
