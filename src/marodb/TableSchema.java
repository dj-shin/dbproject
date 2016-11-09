/**
 * Created by LastOne on 2016-10-28.
 */
package marodb;

import marodb.exceptions.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;

public class TableSchema implements Serializable {
    private String tableName;
    private LinkedHashMap<String, Field> fields;

    public TableSchema(String tableName, LinkedHashMap<String, Field> fields) {
        this.tableName = tableName;
        this.fields = fields;
    }

    public final String getTableName() {
        return tableName;
    }

    public LinkedHashMap<String, marodb.Field> getFields() {
        return fields;
    }

    private int pkCount() {
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