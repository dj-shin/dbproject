package marodb.predicate;

import marodb.exceptions.WhereIncomparableError;
import marodb.type.*;

/**
 * Created by LastOne on 2016-11-19.
 */
public class CompareOperation {
    public static final String LEQ = "<=";
    public static final String GEQ = ">=";
    public static final String LT =  "<";
    public static final String GT =  ">";
    public static final String EQ =  "=";
    public static final String NEQ = "!=";

    private String op;

    /**
     * Comparision operator
     * @param op
     */
    public CompareOperation(String op) {
        this.op = op;
    }

    /**
     * Evaluate comparison between two values
     * @param a
     * @param b
     * @return
     */
    public ThreeValuedLogic eval(Value a, Value b) {
        // Either value is null, return unknown
        if (a.getType().getType().equals(DataType.NULLTYPE) || b.getType().getType().equals(DataType.NULLTYPE)) {
            return new ThreeValuedLogic("UNKNOWN");
        }
        // Both values are char
        if (a instanceof CharValue && b instanceof CharValue) {
            if (eval(((CharValue) a).getValue(), ((CharValue) b).getValue())) {
                return new ThreeValuedLogic("TRUE");
            }
            else {
                return new ThreeValuedLogic("FALSE");
            }
        }
        // Both values are date
        else if (a instanceof DateValue && b instanceof DateValue) {
            if (eval(((DateValue) a).getValue(), ((DateValue) b).getValue())) {
                return new ThreeValuedLogic("TRUE");
            }
            else {
                return new ThreeValuedLogic("FALSE");
            }
        }
        // Both values are integer
        else if (a instanceof IntValue && b instanceof IntValue) {
            if (eval(((IntValue) a).getValue(), ((IntValue) b).getValue())) {
                return new ThreeValuedLogic("TRUE");
            }
            else {
                return new ThreeValuedLogic("FALSE");
            }
        }
        // Type mismatch
        else {
            throw new WhereIncomparableError();
        }
    }

    // Evaluate on string values
    private boolean eval(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        if (op.equals(LEQ)) {
            return a.compareToIgnoreCase(b) <= 0;
        } else if (op.equals(GEQ)) {
            return a.compareToIgnoreCase(b) >= 0;
        } else if (op.equals(LT)) {
            return a.compareToIgnoreCase(b) < 0;
        } else if (op.equals(GT)) {
            return a.compareToIgnoreCase(b) > 0;
        } else if (op.equals(EQ)) {
            return a.compareToIgnoreCase(b) == 0;
        } else if (op.equals(NEQ)) {
            return a.compareToIgnoreCase(b) != 0;
        } else {
            throw new RuntimeException("Invalid operation");
        }
    }

    // Evaluate on integer values
    private boolean eval(Integer aObj, Integer bObj) {
        if (aObj == null || bObj == null) {
            return false;
        }
        int a = aObj.intValue();
        int b = bObj.intValue();
        if (op.equals(LEQ)) {
            return a <= b;
        } else if (op.equals(GEQ)) {
            return a >= b;
        } else if (op.equals(LT)) {
            return a < b;
        } else if (op.equals(GT)) {
            return a > b;
        } else if (op.equals(EQ)) {
            return a == b;
        } else if (op.equals(NEQ)) {
            return a != b;
        } else {
            throw new RuntimeException("Invalid operation");
        }
    }

    @Override
    public String toString() {
        return op;
    }
}
