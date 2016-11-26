package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-27.
 */
public class SelectColumnResolveError extends SelectionError {
    String column;

    public SelectColumnResolveError(String column) {
        this.column = column;
    }

    @Override
    public String getMessage() {
        return "fail to resolve '" + column + "'";
    }
}
