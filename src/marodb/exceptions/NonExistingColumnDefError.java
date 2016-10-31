package marodb.exceptions;

/**
 * Created by LastOne on 2016-10-31.
 */
public class NonExistingColumnDefError extends QueryError {
    private String columnName;

    public NonExistingColumnDefError(String columnName) {
        this.columnName = columnName;
    }

    @Override
    public String errorMessage() {
        return "'" + columnName + "' does not exists in column definition";
    }
}
