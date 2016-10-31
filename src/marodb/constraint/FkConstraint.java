package marodb.constraint;

import java.util.ArrayList;

/**
 * Created by LastOne on 2016-10-31.
 */
public class FkConstraint implements Constraint {
    private ArrayList<String> columnList;
    private String table;
    private ArrayList<String> referenceList;

    public FkConstraint(ArrayList<String> columnList, String table, ArrayList<String> referenceList) {
        this.columnList = columnList;
        this.table = table;
        this.referenceList = referenceList;
    }

    public final ArrayList<String> getColumnList() {
        return columnList;
    }

    public final String getTable() {
        return table;
    }

    public final ArrayList<String> getReferenceList() {
        return referenceList;
    }

    @Override
    public String getType() {
        return FK;
    }
}
