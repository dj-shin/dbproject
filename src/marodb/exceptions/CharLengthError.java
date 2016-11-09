package marodb.exceptions;

/**
 * Created by LastOne on 2016-10-31.
 */
public class CharLengthError extends SchemaError {
    @Override
    public String getMessage() {
        return "Char length should be over 0";
    }
}
