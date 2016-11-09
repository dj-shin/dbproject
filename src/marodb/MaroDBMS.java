package marodb;

import java.io.File;
import java.io.UnsupportedEncodingException;
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
import marodb.type.CharType;
import marodb.type.DataType;
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
    private static EntryBinding dataBinding = null;

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
        dataBinding = new SerialBinding(classCatalog, TableSchema.class);
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
                        fkColumn.setFk(tableName, refColumnName);
                    }
                }
            }
            // All constraints satisfied, create new table
            DatabaseEntry tables = new DatabaseEntry(TABLES.getBytes("UTF-8"));
            DatabaseEntry newTable = new DatabaseEntry();
            dataBinding.objectToEntry(new TableSchema(tablename, schema), newTable);
            cursor.put(tables, newTable);
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
                TableSchema foundTable = (TableSchema) dataBinding.entryToObject(foundData);
                for (Field field: foundTable.getFields().values()) {
                    if (field.getFk() != null && field.getFk().first().equals(tableName)) {
                        throw new DropReferencedTableError(tableName);
                    }
                }
            } while ( cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS );
            cursor.close();

            // Delete table schema
            // Deletion of records are required afterwards
            cursor = myDatabase.openCursor(null, null);
            cursor.getFirst(foundKey, foundData, LockMode.DEFAULT);
            do {
                TableSchema foundTable = (TableSchema) dataBinding.entryToObject(foundData);
                if (foundTable.getTableName().equals(tableName)) {
                    cursor.delete();
                    break;
                }
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
                TableSchema foundTable = (TableSchema) dataBinding.entryToObject(foundData);
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
                TableSchema data = (TableSchema) dataBinding.entryToObject(foundData);
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

    public void close() {
        // Closing Database
        if (myDatabase != null) myDatabase.close();
        if (classDatabase != null) classDatabase.close();
        if (myDbEnvironment != null) myDbEnvironment.close();
    }
}
