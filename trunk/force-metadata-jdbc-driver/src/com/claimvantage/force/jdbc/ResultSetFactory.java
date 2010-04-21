package com.claimvantage.force.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds a force.com org's objects (tables) and fields (columns)
 * and translates them into ResultSet objects that match the patterns
 * specified in the DatabaseMetaData Javadoc.
 */
public class ResultSetFactory {
    
    private List<Table> tables = new ArrayList<Table>();
    private int counter;     

    public ResultSetFactory() {
        /*
        List<Column> columns1 = new ArrayList<Column>();
        columns1.add(new Column("Id", Types.INTEGER));
        columns1.add(new Column("TABLE_ONE_COLUMN_ONE", Types.VARCHAR));
        columns1.add(new Column("TABLE_ONE_COLUMN_TWO", Types.INTEGER));
        columns1.add(new Column("TABLE_ONE_COLUMN_THREE_Id", Types.INTEGER, "TABLE_TWO", "Id"));
        columns1.add(new Column("TABLE_ONE_COLUMN_FOUR", Types.INTEGER));
        Table t1 = new Table("TABLE_ONE", "Remarks one", columns1);
        tables.add(t1);
        
        List<Column> columns2 = new ArrayList<Column>();
        columns2.add(new Column("Id", Types.INTEGER));
        columns2.add(new Column("TABLE_TWO_COLUMN_ONE", Types.VARCHAR));
        columns2.add(new Column("TABLE_TWO_COLUMN_TWO", Types.INTEGER));
        Table t2 = new Table("TABLE_TWO", "Remarks two", columns2);
        tables.add(t2);
        */
    }
    
    public void addTable(Table table) {
        tables.add(table);
    }
    
    /**
     * Provide table (object) detail.
     */
    public ResultSet getTables() {
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (Table table : tables) {
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("TABLE_NAME", table.getName());
            map.put("TABLE_TYPE", "TABLE");
            map.put("REMARKS", table.getComments());
            maps.add(map);
        }
        return new ForceResultSet(maps);
    }
    
    /**
     * Provide column (field) detail.
     */
    public ResultSet getColumns(String tableName) {
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (Table table : tables) {
            if (table.getName().equals(tableName)) {
                int ordinal = 1;
                for (Column column : table.getColumns()) {
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("TABLE_NAME", table.getName());
                    map.put("COLUMN_NAME", column.getName());
                    map.put("DATA_TYPE", Types.OTHER);
                    map.put("TYPE_NAME", column.getType());
                    map.put("ORDINAL_POSITION", ordinal++);
                    map.put("COLUMN_SIZE", column.getLength());
                    map.put("DECIMAL_DIGITS", 0);
                    map.put("REMARKS", column.getComments());
                    map.put("NULLABLE", column.isNillable() ? DatabaseMetaData.columnNullable : DatabaseMetaData.columnNoNulls);
                    
                    // The Auto column is obtained by SchemaSpy via ResultSetMetaData so awkward to support
                    
                    maps.add(map);
                }
            }
        }
        return new ForceResultSet(maps);
    }
    
    /**
     * Provide table (object) relationship information.
     */
    public ResultSet getImportedKeys(String tableName) {
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (Table table : tables) {
            if (table.getName().equals(tableName)) {
                for (Column column : table.getColumns()) {
                    if (column.getReferencedTable() != null && column.getReferencedColumn() != null) {
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("PKTABLE_NAME", column.getReferencedTable());
                        map.put("PKCOLUMN_NAME", column.getReferencedColumn());
                        map.put("FKTABLE_NAME", tableName);
                        map.put("FKCOLUMN_NAME", column.getName());
                        map.put("FK_NAME", "FakeFK" + counter);
                        map.put("PK_NAME", "FakePK" + counter);
                        counter++;
                        maps.add(map);
                    }
                }
            }
        }
        return new ForceResultSet(maps);
    }
    
    /**
     * May not be needed.
     */
    public ResultSet getPrimaryKeys(String tableName)
            throws SQLException {
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (Table table : tables) {
            if (table.getName().equals(tableName)) {
                for (Column column : table.getColumns()) {
                    if (column.getName().equalsIgnoreCase("Id")) {
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("TABLE_NAME", table.getName());
                        map.put("COLUMN_NAME", "" + column.getName());
                        map.put("KEY_SEQ", 0);
                        map.put("PK_NAME", "FakePK" + counter);
                        maps.add(map);
                    }
                }
            }
        }
        return new ForceResultSet(maps);
    }
    
    /**
     * Avoid the tables (objects) appearing in the "tables without indexes" anomalies list.
     */
    public ResultSet getIndexInfo(String tableName)
            throws SQLException {
        List<Map<String, Object>> maps = new ArrayList<Map<String, Object>>();
        for (Table table : tables) {
            if (table.getName().equals(tableName)) {
                for (Column column : table.getColumns()) {
                    if (column.getName().equalsIgnoreCase("Id")) {
                        Map<String, Object> map = new HashMap<String, Object>();
                        map.put("INDEX_NAME", "FakeIndex" + counter++);
                        map.put("TABLE_NAME", table.getName());
                        map.put("COLUMN_NAME", "Id");
                        map.put("NON_UNIQUE", false);
                        map.put("TYPE", DatabaseMetaData.tableIndexOther);
                        maps.add(map);
                    }
                }
            }
        }
        return new ForceResultSet(maps);
    }
}
