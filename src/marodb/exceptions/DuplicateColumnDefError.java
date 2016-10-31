package marodb.exceptions;

/**
 * Created by LastOne on 2016-10-31.
 */
public class DuplicateColumnDefError extends QueryError {
    @Override
    public String errorMessage() {
        return "column definition is duplicated";
    }
}
