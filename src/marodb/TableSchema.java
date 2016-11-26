/**
 * Created by LastOne on 2016-10-28.
 */
package marodb;

import marodb.constraint.FkConstraint;
import marodb.exceptions.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class TableSchema implements Serializable {
    private final String tableName;
    private final LinkedHashMap<String, Field> fields;
    private final ArrayList<FkConstraint> fkConstraints;

    public TableSchema(String tableName, LinkedHashMap<String, Field> fields, ArrayList<FkConstraint> fkConstraints) {
        this.tableName = tableName;
        this.fields = fields;
        this.fkConstraints = fkConstraints;
    }

    public final ArrayList<FkConstraint> getFkConstraints() {
        return fkConstraints;
    }

    public final String getTableName() {
        return tableName;
    }

    public final LinkedHashMap<String, marodb.Field> getFields() {
        return fields;
    }

    public ArrayList<String> getColumns() {
        ArrayList<String> columns = new ArrayList<String>();
        for (String column : fields.keySet()) {
            columns.add(column);
        }
        return columns;
    }

    public int pkCount() {
        int count = 0;
        for (Field field : fields.values()) {
            if (field.getPk()) {
                count++;
            }
        }
        return count;
    }

    public void checkPk(ArrayList<String> columnList) throws QueryError {
        // Check for duplicate
        Set<String> set = new HashSet<String>(columnList);
        if (set.size() != columnList.size()) {
            throw new ReferenceDuplicateError();
        }
        // Valid column && pk
        for (String column : columnList) {
            if ( !fields.containsKey(column) ) {
                throw new ReferenceColumnExistenceError();
            }
            if ( !fields.get(column).getPk() ) {
                throw new ReferenceNonPrimaryKeyError();
            }
        }
    }

    public String toString() {
        return "[table: " + tableName + " | fields : " + fields.toString() + "]";
    }
}