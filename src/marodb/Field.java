package marodb;

import marodb.type.DataType;
import marodb.util.Pair;

import java.io.Serializable;

/**
 * Created by LastOne on 2016-10-30.
 */
public class Field implements Serializable {
    private String columnName;
    private DataType type;
    private boolean not_null;
    private boolean pk;
    private Pair<String, String> fk;

    public Field(String columnName, DataType type, boolean not_null) {
        this.columnName = columnName;
        this.type = type;
        this.not_null = not_null;
        this.pk = false;
        this.fk = null;
    }

    public DataType getType() {
        return type;
    }

    public void setPk() {
        pk = true;
        not_null = true;
    }

    public boolean getPk() {
        return pk;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setFk(String table, String column) {
        fk = new Pair<String, String>(table, column);
    }

    public Pair<String, String> getFk() {
        return fk;
    }

    @Override
    public final String toString() {
        String is_null, key = "";
        if (not_null) {
            is_null = "N";
        } else {
            is_null = "Y";
        }

        if (fk != null) {
            if (pk) {
                key = "PRI/FOR";
            } else {
                key = "FOR";
            }
        } else {
            if (pk) {
                key = "PRI";
            }
        }
        return columnName + "\t" + type.toString() + "\t" + is_null + "\t" + key;
    }
}
