package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-25.
 */
public class InsertReferentialIntegrityError extends InsertionError {
    @Override
    public String getMessage() {
        return "Referential integrity violation";
    }
}
