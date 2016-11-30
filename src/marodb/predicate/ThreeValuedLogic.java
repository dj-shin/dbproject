package marodb.predicate;

/**
 * Created by LastOne on 2016-11-26.
 */
public class ThreeValuedLogic {
    public static int TRUE = 1;
    public static int UNKNOWN = 0;
    public static int FALSE = -1;

    private final int value;

    public ThreeValuedLogic(String type) {
        if (type.equals("TRUE")) {
            value = TRUE;
        }
        else if (type.equals("FALSE")) {
            value = FALSE;
        }
        else if (type.equals("UNKNOWN")){
            value = UNKNOWN;
        }
        else {
            throw new RuntimeException("Wrong type of three valued logic : " + type);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ThreeValuedLogic) {
            ThreeValuedLogic other = (ThreeValuedLogic)obj;
            return other.value == value;
        }
        return false;
    }

    public ThreeValuedLogic or(ThreeValuedLogic other) {
        if (value == TRUE || other.value == TRUE) {
            return new ThreeValuedLogic("TRUE");
        }
        else if (value == FALSE && other.value == FALSE) {
            return new ThreeValuedLogic("FALSE");
        }
        else {
            return new ThreeValuedLogic("UNKNOWN");
        }
    }

    public ThreeValuedLogic and(ThreeValuedLogic other) {
        if (value == FALSE || other.value == FALSE) {
            return new ThreeValuedLogic("FALSE");
        }
        else if (value == TRUE && other.value == TRUE) {
            return new ThreeValuedLogic("TRUE");
        }
        else {
            return new ThreeValuedLogic("UNKNOWN");
        }
    }

    public ThreeValuedLogic not() {
        if (value == TRUE) {
            return new ThreeValuedLogic("FALSE");
        }
        else if (value == FALSE) {
            return new ThreeValuedLogic("TRUE");
        }
        else {
            return new ThreeValuedLogic("UNKNOWN");
        }
    }

    @Override
    public String toString() {
        if (value == TRUE) {
            return "TRUE";
        }
        else if (value == FALSE) {
            return "FALSE";
        }
        else {
            return "UNKNOWN";
        }
    }
}
