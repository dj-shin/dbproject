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

    public BooleanTest(Predicate predicate) {
        this.predicate = predicate;
        isExp = false;
        not = false;
    }

    public BooleanTest(BooleanExp booleanExp) {
        this.booleanExp = booleanExp;
        isExp = true;
    }

    public void setNot(boolean not) {
        this.not = not;
    }

    @Override
    public String toString() {
        if (isExp) {
            return booleanExp.toString();
        } else {
            return (not ? "not " : "") + predicate.toString();
        }
    }

    public ThreeValuedLogic eval(LinkedHashMap<Column, Value> record) {
        if (isExp) {
            ThreeValuedLogic result = booleanExp.eval(record);
            return (not ? result.not() : result);
        }
        else {
            ThreeValuedLogic result = predicate.eval(record);
            return (not ? result.not() : result);
        }
    }
}
