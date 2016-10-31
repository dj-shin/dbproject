/**
 * Created by LastOne on 2016-10-28.
 */
package marodb;

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

    public boolean isCorrectPk(ArrayList<String> columnList) {
        // Check for duplicate
        Set<String> set = new HashSet<String>(columnList);
        if (set.size() != columnList.size()) {
            System.out.println("Duplicate columnList");
            return false;
        }
        if (columnList.size() != pkCount()) {
            System.out.println("Not full pkList");
            return false;
        }
        // Valid column && pk
        for (String column : columnList) {
            if ( !fields.containsKey(column) || !fields.get(column).getPk()) {
                System.out.println("Invalid key " + column);
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return "[table: " + tableName + " | fields : " + fields.toString() + "]";
    }
}