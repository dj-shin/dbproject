package marodb.exceptions;

/**
 * Created by LastOne on 2016-10-31.
 */
public class DropReferencedTableError extends QueryError {
    private String tableName;

    public DropReferencedTableError(String tableName) {
        this.tableName = tableName;
    }

    @Override
    public String errorMessage() {
        return "'" + tableName + "' is referenced by other table";
    }
}
