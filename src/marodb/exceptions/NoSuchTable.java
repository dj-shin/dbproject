package marodb.exceptions;

/**
 * Created by LastOne on 2016-10-31.
 */
public class NoSuchTable extends QueryError {
    @Override
    public String getMessage() {
        return "No such table";
    }
}
