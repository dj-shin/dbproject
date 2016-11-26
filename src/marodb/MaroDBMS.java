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
import marodb.predicate.BooleanExp;
import marodb.predicate.ThreeValuedLogic;
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
            ArrayList<FkConstraint> fkConstraints = new ArrayList<FkConstraint>();
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
                    fkConstraints.add(fkConstraint);
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
            TableSchema tableSchema = new TableSchema(tablename, schema, fkConstraints);
            schemaBinding.objectToEntry(tableSchema, newTable);
            cursor.put(tables, newTable);

            DatabaseEntry tableName = new DatabaseEntry(tablename.getBytes("UTF-8"));
            DatabaseEntry newRelation = new DatabaseEntry();
            ArrayList<Column> relationSchema = new ArrayList<Column>();
            for (String column : tableSchema.getColumns()) {
                relationSchema.add(new Column(tablename, column));
            }
            relationBinding.objectToEntry(new Relation(tablename, relationSchema), newRelation);
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
                Relation foundRelation = (Relation) relationBinding.entryToObject(foundData);
                if (foundRelation.getName().equals(tableName)) {
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
                if (foundRelation.getName().equals(tableName)) {
                    return foundRelation;
                }
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
        // Start joining from first table
        Relation r = findRelation(from.get(0).first());
        if (r == null) {
            // If table doesn't exists
            throw new SelectTableExistencError(from.get(0).first());
        }
        Relation result = r.renameTable(from.get(0).second());
        for (int tableIndex = 1; tableIndex < from.size(); tableIndex++) {
            Relation fromRelation = findRelation(from.get(tableIndex).first());
            if (fromRelation == null) {
                throw new SelectTableExistencError(from.get(0).first());
            }
            result = result.join(fromRelation.renameTable(from.get(tableIndex).second()));
        }
        if (where != null) {
            result = result.select(where);
        }
        if (selectList != null) {
            ArrayList<Column> columnList = selectList.first();
            ArrayList<String> asList     = selectList.second();
            result = result.renameColumn(columnList, asList);
        }
        result.pprint();
    }

    /**
     * Process insert query
     * @param table
     * @param columnNameList    nullable
     * @param valueList
     * @throws QueryError
     */
    public void insert(String table, ArrayList<String> columnNameList, ArrayList<Value> valueList) throws InsertionError {
        TableSchema tableSchema = findTable(table);
        if (tableSchema == null) {
            // If table doesn't exists
            throw new NoSuchTable();
        }

        if (columnNameList == null) {
            // If there is no explicit column list, create from table schema
            columnNameList = new ArrayList<String>();
            for (String column : tableSchema.getFields().keySet()) {
                columnNameList.add(column);
            }
        }
        if (columnNameList.size() != valueList.size()) {
            // If number of column and value doesn't match
            throw new InsertTypeMismatchError();
        }
        int index = 0;
        boolean pkDuplicate = true;
        boolean hasPk = false;
        LinkedHashMap<Column, Value> record = new LinkedHashMap<Column, Value>();
        Relation relation = findRelation(table);
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

            // primary duplicate check
            if (field.getPk()) {
                hasPk = true;
                if (!relation.hasRecord(column, v)) {
                    pkDuplicate = false;
                }
            }

            record.put(new Column(table, column), v);
            index++;
        }

        if (hasPk && pkDuplicate) {
            throw new InsertDuplicatePrimaryKeyError();
        }

        for (FkConstraint fkConstraint : tableSchema.getFkConstraints()) {
            boolean hasNull = false;
            Relation foreignRelation = findRelation(fkConstraint.getTable());
            for (int fkIndex = 0; fkIndex < fkConstraint.getColumnList().size(); fkIndex++) {
                Column column = new Column(table, fkConstraint.getColumnList().get(fkIndex));
                Value v = record.get(column);
                if (v.getType().getType().equals(DataType.NULLTYPE)) {
                    hasNull = true;
                    break;
                }
            }
            if (!hasNull && !foreignRelation.referenceable(table, record, fkConstraint)) {
                throw new InsertReferentialIntegrityError();
            }
        }

        relation.addRecord(record);
        updateRelation(table, relation);
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
                Relation foundRelation = (Relation) relationBinding.entryToObject(foundData);
                if (foundRelation.getName().equals(table)) {
                    cursor.delete();
                    DatabaseEntry newRelation = new DatabaseEntry();
                    relationBinding.objectToEntry(relation, newRelation);
                    cursor.put(foundKey, newRelation);
                    break;
                }
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

    public boolean cascade(String tableName, LinkedHashMap<Column, Value> record) {
        Cursor cursor = myDatabase.openCursor(null, null);
        ArrayList<Pair<String, Relation>> buffer = new ArrayList<Pair<String, Relation>>();

        try {
            DatabaseEntry foundKey = new DatabaseEntry(TABLES.getBytes("UTF-8"));
            DatabaseEntry foundData = new DatabaseEntry();

            cursor.getFirst(foundKey, foundData, LockMode.DEFAULT);
            if (foundData.getData() == null) {
                return true;
            }
            do {
                if (!(schemaBinding.entryToObject(foundData) instanceof TableSchema)) {
                    continue;
                }
                TableSchema table = (TableSchema) schemaBinding.entryToObject(foundData);
                for (FkConstraint fkConstraint : table.getFkConstraints()) {
                    if (fkConstraint.getTable().equals(tableName)) {
                        for (String column : fkConstraint.getColumnList()) {
                            if (!table.getFields().get(column).nullable()) {
                                return false;
                            }
                        }
                        // Replace with null
                        Relation r = findRelation(table.getTableName())
                                .deleteReference(fkConstraint, record);
                        buffer.add(new Pair<String, Relation>(table.getTableName(), r));
                    }
                }
            } while ( cursor.getNext(foundKey, foundData, LockMode.DEFAULT) == OperationStatus.SUCCESS );
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } finally {
            cursor.close();
        }
        for (Pair<String, Relation> p : buffer) {
            String t = p.first();
            Relation r = p.second();
            updateRelation(t, r);
        }
        return true;
    }

    public void delete(String table, BooleanExp where) {
        Relation relation = findRelation(table);
        Relation result = new Relation(table, relation.getSchema());
        if (relation == null) {
            throw new NoSuchTable();
        }
        int deletedCount = 0, deleteFailCount = 0;
        for (LinkedHashMap<Column, Value> record : relation.getInstance()) {
            if (where == null || where.eval(record).equals(new ThreeValuedLogic("TRUE"))) {
                if (cascade(table, record)) {
                    deletedCount++;
                }
                else {
                    deleteFailCount++;
                    result.addRecord(record);
                }
            }
            else {
                result.addRecord(record);
            }
        }
        updateRelation(table, result);
        System.out.println(String.valueOf(deletedCount) + " row(s) are deleted");
        if (deleteFailCount > 0) {
            System.out.println(String.valueOf(deleteFailCount) + " row(s) are not deleted due to referential integrity");
        }
    }
}
