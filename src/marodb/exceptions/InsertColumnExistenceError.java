package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-25.
 */
public class InsertColumnExistenceError extends InsertionError {
    String columnName;

    public InsertColumnExistenceError(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public String getMessage() {
        return "'" + columnName + "' does not exist";
    }
}
