package marodb.predicate;

import marodb.exceptions.WhereAmbiguousReference;
import marodb.exceptions.WhereColumnNotExist;
import marodb.relation.Column;
import marodb.type.DataType;
import marodb.type.Value;
import marodb.util.Pair;

import java.util.LinkedHashMap;

/**
 * Created by LastOne on 2016-11-19.
 */
public class NullPredicate implements Predicate {
    private Column column;
    private boolean isNull;

    public NullPredicate(Column column, boolean isNull) {
        this.column = column;
        this.isNull = isNull;
    }

    @Override
    public String toString() {
        return column.toString() + " is " + (isNull ? "null" : "not null");
    }

    public ThreeValuedLogic eval(LinkedHashMap<Column, Value> record) {
        int numMatchColumn = 0;
        Column matchColumn = null;
        for (Column c : record.keySet()) {
            if (c.equals(column)) {
                numMatchColumn++;
                matchColumn = c;
            }
        }
        if (numMatchColumn == 0) {
            throw new WhereColumnNotExist();
        }
        else if (numMatchColumn > 1) {
            throw new WhereAmbiguousReference();
        }
        if (record.get(matchColumn).getType().getType().equals(DataType.NULLTYPE)) {
            return (isNull ? new ThreeValuedLogic("TRUE") : new ThreeValuedLogic("FALSE"));
        }
        else {
            return (isNull ? new ThreeValuedLogic("FALSE") : new ThreeValuedLogic("TRUE"));
        }
    }
}
