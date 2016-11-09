package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-01.
 */
public class ReferenceDuplicateError extends QueryError {
    @Override
    public String getMessage() {
        return "duplicate columns in foreign key constraint";
    }
}
