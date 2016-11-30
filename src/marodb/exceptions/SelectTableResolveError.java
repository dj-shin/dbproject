package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-27.
 */
public class SelectTableResolveError extends SelectionError {
    String table;

    public SelectTableResolveError(String table) {
        this.table = table;
    }

    @Override
    public String getMessage() {
        return "Not unique table/alias '" + table + "'";
    }
}
