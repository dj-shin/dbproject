package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-25.
 */
public class WhereIncomparableError extends WhereError {
    @Override
    public String getMessage() {
        return "Where clause try to compare incomparable values";
    }
}
