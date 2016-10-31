package marodb.exceptions;

/**
 * Created by LastOne on 2016-10-31.
 */
public class TableExistenceError extends QueryError {
    @Override
    public String errorMessage() {
        return "table with the same name already exists";
    }
}
