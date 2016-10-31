package marodb.constraint;

import java.util.ArrayList;

/**
 * Created by LastOne on 2016-10-31.
 */
public class PkConstraint implements Constraint {
    private ArrayList<String> columnList;

    public PkConstraint(ArrayList<String> columnList) {
        this.columnList = columnList;
    }

    public final ArrayList<String> getColumnList() {
        return columnList;
    }

    @Override
    public String getType() {
        return PK;
    }
}
