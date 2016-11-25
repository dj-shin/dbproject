package marodb.predicate;

import marodb.exceptions.WhereAmbiguousReference;
import marodb.exceptions.WhereColumnNotExist;
import marodb.relation.Column;
import marodb.type.Value;
import marodb.util.Pair;

import java.util.LinkedHashMap;

/**
 * Created by LastOne on 2016-11-20.
 */
public class CompareOperand {
    private Value v;
    private Column column;
    private final boolean isValue;

    public CompareOperand(Value v) {
        this.v = v;
        isValue = true;
    }

    public CompareOperand(Column column) {
        this.column = column;
        isValue = false;
    }

    @Override
    public String toString() {
        if (isValue) {
            return v.toString();
        } else {
            return column.toString();
        }
    }

    public Value eval(LinkedHashMap<Column, Value> record) {
        if (isValue) {
            return v;
        }
        else {
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
            } else if (numMatchColumn > 1) {
                throw new WhereAmbiguousReference();
            }
            return record.get(matchColumn);
        }
    }
}
