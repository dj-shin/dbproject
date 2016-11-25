package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-25.
 */
public class InsertColumnNonNullableError extends InsertionError {
    String columnName;

    public InsertColumnNonNullableError(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public String getMessage() {
        return "'" + columnName + "' is not nullable";
    }
}
