package marodb.predicate;

import marodb.exceptions.WhereAmbiguousReference;
import marodb.exceptions.WhereColumnNotExist;
import marodb.exceptions.WhereTableNotSpecified;
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

    /**
     * Operand of value
     * @param v
     */
    public CompareOperand(Value v) {
        this.v = v;
        isValue = true;
    }

    /**
     * Operand of column
     * @param column
     */
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

    /**
     * Evaluation of value with context of record
     * @param record
     * @return
     */
    public Value eval(LinkedHashMap<Column, Value> record) {
        if (isValue) {
            return v;
        }
        else {
            // Count matched columns
            int numMatchColumn = 0;
            Column matchColumn = null;
            for (Column c : record.keySet()) {
                if (c.equals(column)) {
                    numMatchColumn++;
                    matchColumn = c;
                }
            }
            if (numMatchColumn == 0) {
                // No column matched
                boolean tableFound = false;
                if (column.getTable() != null) {
                    for (Column c : record.keySet()) {
                        if (c.getTable().equals(column.getTable())) {
                            tableFound = true;
                            break;
                        }
                    }
                    if (!tableFound) {
                        throw new WhereTableNotSpecified();
                    }
                }
                throw new WhereColumnNotExist();
            } else if (numMatchColumn > 1) {
                // More than one columns matched
                throw new WhereAmbiguousReference();
            }
            return record.get(matchColumn);
        }
    }
}
