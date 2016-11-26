package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-27.
 */
public class SelectTableExistencError extends SelectionError {
    String table;

    public SelectTableExistencError(String table) {
        this.table = table;
    }

    @Override
    public String getMessage() {
        return "'" + table + "' does not exist";
    }
}
