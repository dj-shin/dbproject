package marodb;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.ref.Reference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.*;
import marodb.constraint.Constraint;
import marodb.constraint.FkConstraint;
import marodb.constraint.PkConstraint;
import marodb.exceptions.*;
import marodb.predicate.BooleanExp;
import marodb.relation.Column;
import marodb.relation.Relation;
import marodb.type.CharType;
import marodb.type.DataType;
import marodb.type.Value;
import marodb.util.Pair;

/**
 * Created by LastOne on 2016-10-28.
 */
public class MaroDBMS {
    // Environment & Database define
    private static Environment myDbEnvironment = null;
    private static Database myDatabase = null;
    private static Database classDatabase = null;
    private static StoredClassCatalog classCatalog = null;
    private static EntryBinding schemaBinding = null;
    private static EntryBinding relationBinding = null;

    private static final String TABLES = "_tables";

    /**
     * Constructor
     */
    public MaroDBMS() {
        // Opening DB
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        myDbEnvironment = new Environment(new File("db/"), envConfig);

        // Open Database or if not, create one
        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setSortedDuplicates(true);
        myDatabase = myDbEnvironment.openDatabase(null, "sampleDatabase", dbConfig);

        dbConfig.setSortedDuplicates(false);
        classDatabase = myDbEnvironment.openDatabase(null, "classDatabase", dbConfig);

        classCatalog = new StoredClassCatalog(classDatabase);
        schemaBinding = new SerialBinding(classCatalog, TableSchema.class);
        relationBinding = new SerialBinding(classCatalog, Relation.class);
    }

    /**
     * Adding table schema to database
     * @param tablename
     * @param fields
     * @throws QueryError
     * @throws SchemaError
     */
    public void addTable(String tablename, Pair<ArrayList<Field>, ArrayList<Constraint>> fields) throws QueryError, SchemaError {
        Cursor cursor = myDatabase.openCursor(null, null);

        // List of <Column Name, Column Data>
        LinkedHashMap<String, Field> schema = new LinkedHashMap<String, Field>();

        try {
            // If there is no table, raise error
            if (findTable(tablename) != null) {
                throw new TableExistenceError();
            }

            // Column Validation
            for (Field field : fields.first()) {
                // If there is no column in such table, raise error
                if ( schema.containsKey(field.getColumnName()) ) {
                    throw new DuplicateColumnDefError();
                }
                // If column type is char, and length <= 0
                if ( field.getType().getType().contains(DataType.CHARTYPE) && ((CharType)field.getType()).getLength() <= 0 ) {
                    throw new CharLengthError();
                }
                schema.put(field.getColumnName(), field);
            }

            // Constraint Validation
            boolean pkAlreadyFound = false;
            for (Constraint constraint : fields.second()) {
                if ( constraint.getType().equals(Constraint.PK) ) {
                    // If duplicate primary constraint definition, raise error
                    if (pkAlreadyFound) {
                        throw new DuplicatePrimaryKeyDefError();
                    }
                    pkAlreadyFound = true;
                    PkConstraint pkConstraint = (PkConstraint) constraint;
                    Set<String> set = new HashSet<String>(pkConstraint.getColumnList());
                    // If duplicate column in primary constraint, raise error
                    if (set.size() != pkConstraint.getColumnList().size()) {
                        throw new ReferenceDuplicateError();
                    }
                    for (String columnName : pkConstraint.getColumnList() ) {
                        Field pkColumn = schema.get(columnName);
                        // If column in primary constraint doesn't exists, raise error
                        if (pkColumn == null) {
                            throw new NonExistingColumnDefError(columnName);
                        }
                        // Valid primary key constraint. Set pk for column.
                        pkColumn.setPk();
                    }
                } else {
                    // Foreign key constraint
                    FkConstraint fkConstraint = (FkConstraint) constraint;
                    String tableName = fkConstraint.getTable();
                    TableSchema table = findTable(tableName);
                    // Referencing table doesn't exists, raise error
                    if (table == null) {
                        throw new ReferenceTableExistenceError();
                    }
                    LinkedHashMap<String, Field> foreignFields = table.getFields();
                    // Column list and reference list doesn't match size, raise error
                    if (fkConstraint.getColumnList().size() != fkConstraint.getReferenceList().size()) {
                        throw new ReferenceTypeError();
                    }
                    table.checkPk(fkConstraint.getReferenceList());
                    Set<String> set = new HashSet<String>(fkConstraint.getColumnList());
                    // Duplicate columns in column list, raise error
                    if (set.size() != fkConstraint.getColumnList().size()) {
                        throw new ReferenceDuplicateError();
                    }
                    for (int i = 0; i < fkConstraint.getColumnList().size(); i++) {
                        String fkColumnName = fkConstraint.getColumnList().get(i);
                        String refColumnName = fkConstraint.getReferenceList().get(i);
                        Field fkColumn = schema.get(fkColumnName);
                        Field refColumn = foreignFields.get(refColumnName);
                        // No referencing column in table, raise error
                        if (refColumn == null) {
                            throw new ReferenceColumnExistenceError();
                        }
                        // No column in creating table, raise error
                        if (fkColumn == null) {
                            throw new NonExistingColumnDefError(fkColumnName);
                        }
                        // Type doesn't match, raise error
                        if (!refColumn.getType().getType().equals(fkColumn.getType().getType())) {
                            throw new ReferenceTypeError();
                        }
                        fkColumn.addFk(tableName, refColumnName);
                    }
                    // If referencing columns are not full primary key, raise error
                    if (fkConstraint.getColumnList().size() != table.pkCount()) {
                        throw new ReferenceNonFullPrimaryKeyError();
                    }
                }
            }
            // All constraints satisfied, create new table
            DatabaseEntry tables = new DatabaseEntry(TABLES.getBytes("UTF-8"));
            DatabaseEntry newTable = new DatabaseEntry();
            schemaBinding.objectToEntry(new TableSchema(tablename, schema), newTable);
            cursor.put(tables, newTable);

            DatabaseEntry tableName = new DatabaseEntry(tablename.getBytes("UTF-8"));
            DatabaseEntry newRelation = new DatabaseEntry();
            relationBinding.objectToEntry(new Relation(), newRelation);
            cursor.put(tableName, newRelation);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (DatabaseException de) {
            de.printStackTrace();
        } catch (QueryError qe) {
            cursor.close();
            throw qe;
        } finally {
            cursor.close();
        }
    }

    public void dropTable(String tableName) throws QueryError {
        TableSchema table = findTable(tableName);
        if (table == null) {
            throw new NoSuchTable();
        }

        Cursor cursor = myDatabase.openCursor(null, null);

        try {
            // Foreign Key constraint check
            DatabaseEntry foundKey = new DatabaseEntry(TABLES.getBytes("UTF-8"));
            DatabaseEntry foundData = new DatabaseEntry();
            cursor.getFirst(foundKey, foundData, LockMode.DEFAULT);
            do {
                if (!(schemaBinding.entryToObject(foundData) instanceof TableSchema)) {
                    continue;
                }
                TableSchema foundTable = (TableSchema) schemaBinding.entryToObject(foundData);
                // For all fields in each table
                for (Field field: foundTable.getFields().values()) {
                    // Find foreign key that references drop target table
                    for (Pair<String, String> fk: field.getFkList()) {
                        if (fk.first().equals(tableName)) {
                            throw new DropReferencedTableError(tableName);
                        }
                    }
                }
            } while ( cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS );
            cursor.close();

            // Delete table schema
            // Deletion of records are required afterwards
            cursor = myDatabase.openCursor(null, null);
            cursor.getFirst(foundKey, foundData, LockMode.DEFAULT);
            do {
                if (!(schemaBinding.entryToObject(foundData) instanceof TableSchema)) {
                    continue;
                }
                TableSchema foundTable = (TableSchema) schemaBinding.entryToObject(foundData);
                if (foundTable.getTableName().equals(tableName)) {
                    cursor.delete();
                    break;
                }
            } while ( cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS );
            cursor.close();

            cursor = myDatabase.openCursor(null, null);
            foundKey = new DatabaseEntry(tableName.getBytes("UTF-8"));
            foundData = new DatabaseEntry();
            cursor.getFirst(foundKey, foundData, LockMode.DEFAULT);
            do {
                if (!(relationBinding.entryToObject(foundData) instanceof Relation)) {
                    continue;
                }
                cursor.delete();
            } while ( cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
    }

    private TableSchema findTable(String tableName) {
        Cursor cursor = myDatabase.openCursor(null, null);

        TableSchema table = null;

        try {
            DatabaseEntry foundKey = new DatabaseEntry(TABLES.getBytes("UTF-8"));
            DatabaseEntry foundData = new DatabaseEntry();
            cursor.getFirst(foundKey, foundData, LockMode.DEFAULT);
            if (foundData.getData() == null) {
                return null;
            }
            do {
                if (!(schemaBinding.entryToObject(foundData) instanceof TableSchema)) {
                    continue;
                }
                TableSchema foundTable = (TableSchema) schemaBinding.entryToObject(foundData);
                if (foundTable.getTableName().equals(tableName)) {
                    return foundTable;
                }
            } while ( cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
        return table;
    }

    private Relation findRelation(String tableName) {
        Cursor cursor = myDatabase.openCursor(null, null);

        try {
            DatabaseEntry foundKey = new DatabaseEntry(tableName.getBytes("UTF-8"));
            DatabaseEntry foundData = new DatabaseEntry();
            cursor.getFirst(foundKey, foundData, LockMode.DEFAULT);
            if (foundData.getData() == null) {
                return null;
            }
            do {
                if (!(relationBinding.entryToObject(foundData) instanceof Relation)) {
                    continue;
                }
                Relation foundRelation = (Relation) relationBinding.entryToObject(foundData);
                return foundRelation;
            } while ( cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
        return null;
    }

    public void showTables() {
        Cursor cursor = myDatabase.openCursor(null, null);

        try {
            DatabaseEntry foundKey = new DatabaseEntry(TABLES.getBytes("UTF-8"));
            DatabaseEntry foundData = new DatabaseEntry();

            cursor.getFirst(foundKey, foundData, LockMode.DEFAULT);
            if (foundData.getData() == null) {
                System.out.println("There is no table");
                return;
            }
            System.out.println("-------------------------------------------------");
            do {
                if (!(schemaBinding.entryToObject(foundData) instanceof TableSchema)) {
                    continue;
                }
                TableSchema data = (TableSchema) schemaBinding.entryToObject(foundData);
                System.out.println(data.getTableName());
            } while ( cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS );
            System.out.println("-------------------------------------------------");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
    }

    public void descTable(String tableName) throws QueryError {
        TableSchema table = findTable(tableName);
        if (table == null) {
            throw new NoSuchTable();
        }
        System.out.println("-------------------------------------------------");
        System.out.println("table_name [" + table.getTableName() + "]");
        System.out.println("column_name\ttype\tnull\tkey");
        for (Field field : table.getFields().values()) {
            System.out.println(field.toString());
        }
        System.out.println("-------------------------------------------------");
    }

    /**
     *
     * @param selectList    nullable
     * @param from
     * @param where         nullable
     */
    public void select(Pair<ArrayList<Column>, ArrayList<String>> selectList,
                       ArrayList<Pair<String, String>> from, BooleanExp where) {
        if (selectList == null) {
            // selct * ...

        }
        else {
            ArrayList<Column> columnList = selectList.first();
            ArrayList<String> asList     = selectList.second();
            for (Pair<String, String> refTable : from) {
                String tableName = refTable.first();
                Relation relation = findRelation(tableName);
                relation.pprint();
            }
        }
    }

    /**
     *
     * @param table
     * @param columnNameList    nullable
     * @param valueList
     * @throws QueryError
     */
    public void insert(String table, ArrayList<String> columnNameList, ArrayList<Value> valueList) throws InsertionError {
        TableSchema tableSchema = findTable(table);
        if (tableSchema == null) {
            throw new NoSuchTable();
        }

        if (columnNameList == null) {
            columnNameList = new ArrayList<String>();
            for (String column : tableSchema.getFields().keySet()) {
                columnNameList.add(column);
            }
        }
        if (columnNameList.size() != valueList.size()) {
            throw new InsertTypeMismatchError();
        }
        int index = 0;
        boolean hasNull = false;
        for (Value v : valueList) {
            if (v.getType().getType().equals(DataType.NULLTYPE)) {
                hasNull = true;
                break;
            }
        }

        LinkedHashMap<Column, Value> record = new LinkedHashMap<Column, Value>();
        for (String column : columnNameList) {
            Value v = valueList.get(index);
            if (!tableSchema.getFields().containsKey(column)) {
                throw new InsertColumnExistenceError(column);
            }
            Field field = tableSchema.getFields().get(column);
            if (!field.getType().getType().equals(v.getType().getType())
                    && !v.getType().getType().equals(DataType.NULLTYPE)) {
                throw new InsertTypeMismatchError();
            }

            if (!field.nullable() && v.getType().getType().equals(DataType.NULLTYPE)) {
                throw new InsertColumnNonNullableError(column);
            }

            if (!hasNull && field.getFkList() != null) {
                for (Pair<String, String> fk : field.getFkList()) {
                    String fkTable = fk.first();
                    String fkColumn = fk.second();
                    Relation foreignRelation = findRelation(fkTable);

                }
            }

            record.put(new Column(table, column), v);
            index++;
        }

        Relation relation = findRelation(table);
        relation.addRecord(record);
        updateRelation(table, relation);
        relation.pprint();
    }

    public void updateRelation(String table, Relation relation) {
        Cursor cursor = myDatabase.openCursor(null, null);
        try {
            // Foreign Key constraint check
            DatabaseEntry foundKey = new DatabaseEntry(table.getBytes("UTF-8"));
            DatabaseEntry foundData = new DatabaseEntry();
            cursor.getFirst(foundKey, foundData, LockMode.DEFAULT);
            do {
                if (!(relationBinding.entryToObject(foundData) instanceof Relation)) {
                    continue;
                }
                DatabaseEntry newRelation = new DatabaseEntry();
                relationBinding.objectToEntry(relation, newRelation);
                cursor.delete();
                cursor.put(foundKey, newRelation);
            } while ( cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
    }

    public void close() {
        // Closing Database
        if (myDatabase != null) myDatabase.close();
        if (classDatabase != null) classDatabase.close();
        if (myDbEnvironment != null) myDbEnvironment.close();
    }
}
