package marodb.exceptions;

/**
 * Created by LastOne on 2016-10-31.
 */
public abstract class QueryError extends RuntimeException {
    public abstract String errorMessage();
}
