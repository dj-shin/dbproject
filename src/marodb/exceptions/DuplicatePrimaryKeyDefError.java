package marodb.exceptions;

/**
 * Created by LastOne on 2016-10-31.
 */
public class DuplicatePrimaryKeyDefError extends QueryError {
    @Override
    public String errorMessage() {
        return "primary key definition is duplicated";
    }
}
