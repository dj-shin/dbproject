package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-25.
 */
public class InsertDuplicatePrimaryKeyError extends InsertionError {
    @Override
    public String getMessage() {
        return "Primary key duplication";
    }
}
