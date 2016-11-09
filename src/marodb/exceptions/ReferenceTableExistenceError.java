package marodb.exceptions;

/**
 * Created by LastOne on 2016-10-31.
 */
public class ReferenceTableExistenceError extends QueryError {
    @Override
    public String getMessage() {
        return "foreign key references non existing table";
    }
}
