package marodb.predicate;

import marodb.relation.Column;
import marodb.type.Value;

import java.util.LinkedHashMap;

/**
 * Created by LastOne on 2016-11-20.
 */
public class BooleanTest {
    private Predicate predicate;
    private BooleanExp booleanExp;
    private final boolean isExp;
    private boolean not;

    /**
     * Boolean test of predicate
     * @param predicate
     */
    public BooleanTest(Predicate predicate) {
        this.predicate = predicate;
        isExp = false;
        not = false;
    }

    /**
     * Boolean test of expression
     * @param booleanExp
     */
    public BooleanTest(BooleanExp booleanExp) {
        this.booleanExp = booleanExp;
        isExp = true;
        not = false;
    }

    /**
     * Inverse not
     * @param flip
     */
    public void flipNot(boolean flip) {
        if (flip) {
            this.not = !this.not;
        }
    }

    @Override
    public String toString() {
        if (isExp) {
            return (not ? "not " : "") + booleanExp.toString();
        } else {
            return (not ? "not " : "") + predicate.toString();
        }
    }

    /**
     * Evaluate predicate on given record
     * @param record
     * @return
     */
    public ThreeValuedLogic eval(LinkedHashMap<Column, Value> record) {
        ThreeValuedLogic result;
        if (isExp) {
            result = booleanExp.eval(record);
        }
        else {
            result = predicate.eval(record);
        }
        if (not) {
            return result.not();
        } else {
            return result;
        }
    }
}
