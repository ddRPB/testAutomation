package org.labkey.test.util;

import org.labkey.remoteapi.CommandException;
import org.labkey.remoteapi.Connection;
import org.labkey.remoteapi.collections.CaseInsensitiveHashMap;
import org.labkey.remoteapi.domain.CreateDomainCommand;
import org.labkey.remoteapi.domain.DeleteDomainCommand;
import org.labkey.remoteapi.domain.DeleteDomainResponse;
import org.labkey.remoteapi.domain.DomainResponse;
import org.labkey.remoteapi.domain.GetDomainCommand;
import org.labkey.remoteapi.query.DeleteRowsCommand;
import org.labkey.remoteapi.query.InsertRowsCommand;
import org.labkey.remoteapi.query.SaveRowsResponse;
import org.labkey.remoteapi.query.SelectRowsCommand;
import org.labkey.remoteapi.query.SelectRowsResponse;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;


/* Use this class to generate random test data for a given column schema
* */
public class TestDataGenerator
{
    // chose a Character random from this String
    private static final String ALPHANUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvxyz";

    private final Map<Integer, String> _indices = new HashMap<>();  // used to keep columns and row keys aligned
    private final Map<String, Map<String, Object>> _columns = new CaseInsensitiveHashMap<>();
    private final Map<String, Supplier<Object>> _dataSuppliers = new CaseInsensitiveHashMap<>();
    private List<Map<String, Object>> _rows = new ArrayList<>();
    private int _rowCount = 0;
    private String _schema;
    private String _queryName;
    private String _containerPath;


    /*  use TestDataGenerator to generate data to a specific fieldSet
    *  */
    public TestDataGenerator(String schema, String queryName, String containerPath)
    {
        _schema=schema;
        _queryName=queryName;
        _containerPath = containerPath;
    }

    /*
    this constructor extracts the fieldSet from the specified domain
    * */
    public TestDataGenerator(Connection cn, String schema, String queryName, String containerPath) throws IOException, CommandException
    {
        _schema=schema;
        _queryName=queryName;
        _containerPath = containerPath;
        List<Map<String, Object>> fieldSet = extractDomainFieldSetFrom(cn, schema, queryName, containerPath);
        withColumnSet(fieldSet);   // extract the columns from the specified query
    }

    public String getSchema()
    {
        return _schema;
    }

    public String getQueryName()
    {
        return _queryName;
    }

    /**
     *
     * @param columns   The fieldSet for the domain/sampleset/list.
     * @return
     */
    public TestDataGenerator withColumnSet(List<Map<String, Object>> columns)
    {
        int index = 0;
        for (Map<String, Object> col : columns)
        {
            String columnName = col.get("name").toString();
            _columns.put(columnName, col);                      // todo: encapsulate columns/indices in a class
            _indices.put(index, columnName);                    // and use that everyhere columns are modified or read
            index++;
        }
        return this;
    }

    public TestDataGenerator withGeneratedRows(int desiredRowCount)
    {
        boolean generate = _rowCount == 0;
        _rowCount = desiredRowCount;

        if (generate)
            generateRows();

        return this;
    }

    public TestDataGenerator addDataSupplier(String columnName, Supplier<Object> supplier)
    {
        _dataSuppliers.put(columnName, supplier);
        return this;
    }

    public TestDataGenerator addIntSupplier(String columnName, int min, int max)
    {
        _dataSuppliers.put(columnName, () -> randomInt(min, max));
        return this;
    }

    public TestDataGenerator addStringSupplier(String columnName, int length)
    {
        _dataSuppliers.put(columnName, () -> randomString(length));
        return this;
    }

    /**
     *  // helper to allow adding values as List.of(a, b, c)
     * @param values
     * @return
     */
    public TestDataGenerator addRow(List<Object> values)
    {
        Map<String, Object> row = new HashMap<>();
        for (int i = 0; i < values.size(); i++)     // walk across keys in index order, insert values in that order
        {
            String keyAtIndex = _indices.get(i);
            row.put(keyAtIndex, values.get(i) != "null" ? values.get(i).toString() : null);
        }
        _rows.add(row);
        _rowCount = getRows().size();
        return this;
    }

    /**
     *  Adds the specified row to the internal collection of rowMaps the object contains.
     *  To insert them to the server, use this.
     *  it is acceptable to add columns that don't exist in the destination, but be careful
     * @param customRow use Map.of(colName1, colValue1 ...)
     * @return
     */
    public TestDataGenerator addCustomRow(Map<String, Object> customRow)
    {
        _rows.add(customRow);
        _rowCount = getRows().size();
        return this;
    }

    public List<Map<String, Object>> getRows()
    {
        return _rows;
    }

    public int getRowCount()
    {
        return _rows.size();
    }

    public List<Map<String, Object>> getColumns()
    {
        List<Map<String, Object>> cols = new ArrayList<>();
        for (String key: _columns.keySet())
        {
            cols.add(_columns.get(key));
        }
        return cols;
    }

    public void generateRows()
    {
        if (_columns.keySet().size() == 0)
            throw new IllegalStateException("can't generate row data without column definitions");

        for (int i= 0; i < _rowCount; i++)
        {
            Map<String, Object> newRow = new HashMap<>();

            for (String key : _columns.keySet())
            {
                Map<String, Object> columnDefinition = _columns.get(key);
                // get the column definition
                String columnName = columnDefinition.get("name").toString().toLowerCase();
                String columnType = columnDefinition.get("rangeURI").toString().toLowerCase();

                Object columnValue;
                columnValue = _dataSuppliers.getOrDefault(columnName, getDefaultDataSupplier(columnType)).get();
                newRow.put(columnName, columnValue);
            }
            getRows().add(newRow);
        }
    }

    private Supplier<Object> getDefaultDataSupplier(String columnType)
    {
        switch (columnType)
        {
            case "string":
                return () -> randomString(20);
            case "http://www.w3.org/2001/xmlschema#string":
                return () -> randomString(20);
            case "int":
                return () -> randomInt(0, 20);
            case "http://www.w3.org/2001/xmlschema#int":
            default:
                throw new IllegalArgumentException("ColumnType " + columnType + " isn't implemented yet");
        }
    }

    public String randomString(int size)
    {
        StringBuilder val = new StringBuilder();
        for (int i=0; i<size; i++)
        {
            int randIndex = (int)(ALPHANUMERIC_STRING.length() * Math.random());
            val.append(ALPHANUMERIC_STRING.charAt(randIndex));
        }
        return val.toString();
    }

    public int randomInt(int min, int max)
    {
        if (min >= max)
            throw new IllegalArgumentException("min must be less than max");

        Random r = new Random();
        return r.nextInt((max - min) + 1) + min;
    }

    /*
    * simple way to get a dateformat:  (String)executeScript("return LABKEY.container.formats.dateTimeFormat");
    * */
    public String randomDateString(String dateFormat, Date min, Date max)
    {
        long random = ThreadLocalRandom.current().nextLong(min.getTime(), max.getTime());
        Date date = new Date(random);
        return new SimpleDateFormat(dateFormat).format(date);
    }

    public DomainResponse createDomain(Connection cn, String domainKind) throws IOException, CommandException
    {
        CreateDomainCommand cmd = new CreateDomainCommand(domainKind, getQueryName());
        cmd.setColumns(getColumns());
        return cmd.execute(cn, _containerPath);
    }

    public DeleteDomainResponse deleteDomain(Connection cn) throws IOException, CommandException
    {
        DeleteDomainCommand delCmd = new DeleteDomainCommand(getSchema(), getQueryName());
        return delCmd.execute(cn, _containerPath);
    }

    public SaveRowsResponse insertRows(Connection cn, List<Map<String, Object>> rows) throws IOException, CommandException
    {
        InsertRowsCommand insertRowsCommand = new InsertRowsCommand(getSchema(), getQueryName());
        insertRowsCommand.setRows(rows);
        return insertRowsCommand.execute(cn, _containerPath);
    }

    public SelectRowsResponse getRowsFromServer(Connection cn) throws IOException, CommandException
    {
        return getRowsFromServer(cn, null);
    }

    public SelectRowsResponse getRowsFromServer(Connection cn, List<String> intendedColumns) throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand(getSchema(), getQueryName());
        if (intendedColumns!=null)
            cmd.setColumns(intendedColumns);
        return cmd.execute(cn, _containerPath);
    }

    /**
     *
     * @param cn
     * @param rowsToDelete
     * @return  a list of the rows that were deleted
     * @throws IOException
     * @throws CommandException
     */
    public SaveRowsResponse deleteRows(Connection cn, List<Map<String,Object>> rowsToDelete) throws IOException, CommandException
    {
        DeleteRowsCommand cmd = new DeleteRowsCommand(getSchema(), getQueryName());
        cmd.setRows(rowsToDelete);
        return cmd.execute(cn, _containerPath);
    }

    public TestDataValidator getValidator()
    {
        return new TestDataValidator(_schema, _queryName, _containerPath, _columns, _rows);
    }

    /**
     * The domain fields exclude the standard and magic columns, such as rowId, lsid, parent.
     * @param cn
     * @param schema
     * @param queryName
     * @param containerPath
     * @return
     * @throws IOException
     * @throws CommandException
     */
    static public List<Map<String, Object>> extractDomainFieldSetFrom(Connection cn, String schema, String queryName, String containerPath) throws IOException, CommandException
    {
        GetDomainCommand getDomainCommand = new GetDomainCommand(schema, queryName);
        DomainResponse domainResponse = getDomainCommand.execute(cn, containerPath);
        return domainResponse.getColumns();
    }

    /**
     *  extracts the requested column set from the existing/specified table
     * @param cn
     * @param schema
     * @param queryName
     * @param containerPath
     * @param requestedColumns
     * @return
     * @throws IOException
     * @throws CommandException
     */
    static public List<Map<String, Object>> extractRequestedColumnSet(Connection cn, String schema, String queryName, String containerPath,
                                                                      List<String> requestedColumns)    throws IOException, CommandException
    {
        SelectRowsCommand cmd = new SelectRowsCommand(schema, queryName);
        cmd.setColumns(requestedColumns);
        SelectRowsResponse response = cmd.execute(cn, containerPath);
        return response.getColumnModel();
    }

    // helper to generate a column or field definition
    static public Map<String, Object> simpleFieldDef(String name, String rangeURI)
    {
        Map<String, Object> fieldDef = new HashMap<>();
        fieldDef.put("name", name.toLowerCase());             // column name
        fieldDef.put("rangeURI", rangeURI);                   // column type
        return fieldDef;
    }
}