package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-25.
 */
public abstract class SelectionError extends RuntimeException {
    @Override
    public abstract String getMessage();
}
