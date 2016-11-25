package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-25.
 */
public class InsertTypeMismatchError extends InsertionError {
    @Override
    public String getMessage() {
        return "Types are not matched";
    }
}
