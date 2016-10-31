package marodb.exceptions;

/**
 * Created by LastOne on 2016-10-31.
 */
public class CharLengthError extends SchemaError {
    @Override
    public String errorMessage() {
        return "Char length should be over 0";
    }
}
