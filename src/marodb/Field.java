package marodb;

import marodb.type.DataType;
import marodb.util.Pair;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by LastOne on 2016-10-30.
 */
public class Field implements Serializable {
    private String columnName;
    private DataType type;
    private boolean not_null;
    private boolean pk;
    private ArrayList<Pair<String, String>> fkList;

    public Field(String columnName, DataType type, boolean not_null) {
        this.columnName = columnName;
        this.type = type;
        this.not_null = not_null;
        this.pk = false;
        this.fkList = new ArrayList<Pair<String, String>>();
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

    public void addFk(String table, String column) {
        fkList.add(new Pair<String, String>(table, column));
    }

    public ArrayList<Pair<String, String>> getFkList() {
        return fkList;
    }

    public boolean nullable() {
        return !not_null;
    }

    @Override
    public final String toString() {
        String is_null, key = "";
        if (not_null) {
            is_null = "N";
        } else {
            is_null = "Y";
        }

        if (fkList.size() > 0) {
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
