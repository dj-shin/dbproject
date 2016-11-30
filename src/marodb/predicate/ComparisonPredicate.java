package marodb.predicate;

import marodb.relation.Column;
import marodb.type.DataType;
import marodb.type.Value;

import java.util.LinkedHashMap;

/**
 * Created by LastOne on 2016-11-19.
 */
public class ComparisonPredicate implements Predicate {
    private final CompareOperand a, b;
    private final CompareOperation op;

    /**
     * Comparison predicate
     * @param a
     * @param b
     * @param op
     */
    public ComparisonPredicate(CompareOperand a, CompareOperand b, CompareOperation op) {
        this.a = a;
        this.b = b;
        this.op = op;
    }

    @Override
    public String toString() {
        return a.toString() + " " + op.toString() + " " + b.toString();
    }

    /**
     * Evaluate comparison predicate on record
     * @param record
     * @return
     */
    public ThreeValuedLogic eval(LinkedHashMap<Column, Value> record) {
        Value aValue = a.eval(record);
        Value bValue = b.eval(record);
        return op.eval(aValue, bValue);
    }
}