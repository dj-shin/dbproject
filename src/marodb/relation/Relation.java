package marodb.relation;

import marodb.constraint.FkConstraint;
import marodb.exceptions.SelectColumnResolveError;
import marodb.predicate.BooleanExp;
import marodb.predicate.ThreeValuedLogic;
import marodb.type.NullValue;
import marodb.type.Value;
import marodb.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by LastOne on 2016-11-24.
 */
public class Relation implements Serializable {
    private String name;
    private ArrayList<LinkedHashMap<Column, Value>> instance;
    private ArrayList<Column> schema;

    public ArrayList<Column> getSchema() {
        return schema;
    }

    public Relation(ArrayList<Column> schema) {
        instance = new ArrayList<LinkedHashMap<Column, Value>>();
        this.schema = schema;
    }

    public Relation(String name, ArrayList<Column> schema) {
        this.name = name;
        instance = new ArrayList<LinkedHashMap<Column, Value>>();
        this.schema = schema;
    }

    public void addRecord(LinkedHashMap<Column, Value> record) {
        instance.add(record);
    }

    public ArrayList<LinkedHashMap<Column, Value>> getInstance() {
        return instance;
    }

    public Relation projectionAs(ArrayList<Column> columnList, ArrayList<String> asList) {
        ArrayList<Column> newSchema = new ArrayList<Column>();
        int index = 0;
        for (Column column : columnList) {
            String asName = asList.get(index);
            if (asName == null) {
                newSchema.add(column);
            }
            else {
                newSchema.add(new Column(column.getTable(), asName));
            }
            index++;
        }
        Relation result = new Relation(name, newSchema);
        for (LinkedHashMap<Column, Value> record : instance) {
            LinkedHashMap<Column, Value> projectionRecord = new LinkedHashMap<Column, Value>();
            index = 0;
            for (Column column : columnList) {
                Column renameColumn = column;
                if (asList.get(index) != null) {
                    renameColumn = new Column(null, asList.get(index));
                }
                projectionRecord.put(renameColumn, record.get(column));
                index++;
            }
            result.addRecord(projectionRecord);
        }
        return result;
    }

    public Column identifyColumn(String columnName) {
        Column matchColumn = null;
        for (Column column : schema) {
            if (column.getColumn().equals(columnName)) {
                if (matchColumn != null) {
                    throw new SelectColumnResolveError(columnName);
                }
                matchColumn = column;
            }
        }
        if (matchColumn == null) {
            throw new SelectColumnResolveError(columnName);
        }
        return matchColumn;
    }

    public Relation renameColumn(ArrayList<Column> columns, ArrayList<String> as) {
        ArrayList<Column> identifiedColumns = new ArrayList<Column>();
        for (Column column : columns) {
            if (column.getTable() == null) {
                identifiedColumns.add(identifyColumn(column.getColumn()));
            }
            else {
                if (!schema.contains(column)) {
                    throw new SelectColumnResolveError(column.getColumn());
                }
                identifiedColumns.add(column);
            }
        }
        return projectionAs(identifiedColumns, as);
    }

    public Relation join(Relation other) {
        ArrayList<Column> joinSchema = new ArrayList<Column>();
        for (Column c : schema) {
            joinSchema.add(c);
        }
        for (Column c : other.schema) {
            joinSchema.add(c);
        }
        Relation result = new Relation(joinSchema);

        for (LinkedHashMap<Column, Value> myRecord : instance) {
            for (LinkedHashMap<Column, Value> otherRecord : other.instance) {
                LinkedHashMap<Column, Value> newRecord = new LinkedHashMap<Column, Value>();
                for (Column myColumn : myRecord.keySet()) {
                    newRecord.put(myColumn, myRecord.get(myColumn));
                }
                for (Column otherColumn : otherRecord.keySet()) {
                    newRecord.put(otherColumn, otherRecord.get(otherColumn));
                }
                result.addRecord(newRecord);
            }
        }
        return result;
    }

    public Relation select(BooleanExp predicate) {
        ArrayList<Column> selectSchema = new ArrayList<Column>(schema);
        Relation result = new Relation(selectSchema);

        for (LinkedHashMap<Column, Value> record : instance) {
            if (predicate.eval(record).equals(new ThreeValuedLogic("TRUE"))) {
                result.addRecord(record);
            }
        }
        return result;
    }

    public boolean hasRecord(String table, String columnString, Value value) {
        Column column = new Column(table, columnString);
        for (LinkedHashMap<Column, Value> record : instance) {
            if (record.get(column).equals(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasRecord(String columnString, Value value) {
        Column column = new Column(name, columnString);
        for (LinkedHashMap<Column, Value> record : instance) {
            if (record.get(column).equals(value)) {
                return true;
            }
        }
        return false;
    }

    public boolean referenceable(String table, LinkedHashMap<Column, Value> record, FkConstraint fkConstraint) {
        for (LinkedHashMap<Column, Value> r : instance) {
            boolean result = true;
            int index = 0;
            for (String refColumn : fkConstraint.getReferenceList()) {
                String pkColumn = fkConstraint.getColumnList().get(index);
                if (!record.get(new Column(table, pkColumn))
                        .equals(r.get(new Column(fkConstraint.getTable(), refColumn)))) {
                    result = false;
                    break;
                }
                index++;
            }
            if (result) {
                return true;
            }
        }
        return false;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /**
     * Change relation's table name
     * @param newTable
     * @return
     */
    public Relation renameTable(String newTable) {
        ArrayList<Column> newSchema = new ArrayList<Column>();
        for (Column column : schema) {
            // Change all tables name in schema
            newSchema.add(new Column(newTable, column.getColumn()));
        }
        Relation renameRelation = new Relation(newSchema);
        for (LinkedHashMap<Column, Value> record : instance) {
            LinkedHashMap<Column, Value> newRecord = new LinkedHashMap<Column, Value>();
            for (Column column : record.keySet()) {
                Column renameColumn = new Column(newTable, column.getColumn());
                newRecord.put(renameColumn, record.get(column));
            }
            renameRelation.addRecord(newRecord);
        }
        return renameRelation;
    }

    public Relation deleteReference(FkConstraint fkConstraint, LinkedHashMap<Column, Value> refRecord) {
        Relation result = new Relation(name, schema);
        String table = fkConstraint.getTable();
        for (LinkedHashMap<Column, Value> record : instance) {
            boolean references = true;
            int index = 0;
            LinkedHashMap<Column, Value> updatedRecord = new LinkedHashMap<Column, Value>();
            for (String refColumn: fkConstraint.getReferenceList()) {
                Column column = new Column(name, fkConstraint.getColumnList().get(index));
                if (!refRecord.get(new Column(table, refColumn)).equals(record.get(column))) {
                    references = false;
                }
                index++;
            }
            for (Column c : record.keySet()) {
                if (references && fkConstraint.getColumnList().contains(c.getColumn())) {
                    updatedRecord.put(c, new NullValue());
                }
                else {
                    updatedRecord.put(c, record.get(c));
                }
            }
            result.addRecord(updatedRecord);
        }
        return result;
    }

    public void pprint() {
        if (instance.size() == 0) {
            System.out.println("Empty set");
            return;
        }

        ArrayList<Integer> width = new ArrayList<Integer>();
        int i = 0;
        for (Column column : instance.get(0).keySet()) {
            width.add(column.toString().length() + 2);
        }
        for (i = 0; i < instance.size(); i++) {
            int j = 0;
            for (Column column : instance.get(i).keySet()) {
                if (width.get(j) < instance.get(i).get(column).toString().length() + 2) {
                    width.set(j, instance.get(i).get(column).toString().length() + 2);
                }
                j++;
            }
        }

        for (Integer w : width) {
            System.out.print("+");
            for (i = 0; i < w; i++) {
                System.out.print("-");
            }
        }
        System.out.println("+");
        i = 0;
        for (Column column : instance.get(0).keySet()) {
            System.out.print("| ");
            System.out.print(column.toString());
            for (int j = column.toString().length() + 1; j < width.get(i); j++) {
                System.out.print(" ");
            }
            i++;
        }
        System.out.println("|");
        for (Integer w : width) {
            System.out.print("+");
            for (i = 0; i < w; i++) {
                System.out.print("-");
            }
        }
        System.out.println("+");
        for (LinkedHashMap<Column, Value> record : instance) {
            i = 0;
            for (Value value : record.values()) {
                System.out.print("| ");
                System.out.print(value.toString());
                for (int j = value.toString().length() + 1; j < width.get(i); j++) {
                    System.out.print(" ");
                }
                i++;
            }
            System.out.println("|");
        }
        for (Integer w : width) {
            System.out.print("+");
            for (i = 0; i < w; i++) {
                System.out.print("-");
            }
        }
        System.out.println("+");
    }
}
