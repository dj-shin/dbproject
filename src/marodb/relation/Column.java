package marodb.relation;

import marodb.util.Pair;

import java.io.Serializable;

/**
 * Created by LastOne on 2016-11-24.
 */
public class Column implements Serializable {
    private final String tableName;
    private final String columnName;

    public Column(String tableName, String columnName) {
        this.tableName = tableName;
        this.columnName = columnName;
    }

    public Column(Pair<String, String> column) {
        this.tableName = column.first();
        this.columnName = column.second();
    }

    @Override
    public String toString() {
        return columnName;
    }

    @Override
    public int hashCode() {
        return columnName.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Column) {
            Column other = (Column) obj;
            if ((tableName == other.tableName || tableName == null || other.tableName == null)
                    && columnName == other.columnName) {
                return true;
            }
        }
        return false;
    }
}
