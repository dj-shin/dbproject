package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-25.
 */
public class WhereColumnNotExist extends WhereError {
    @Override
    public String getMessage() {
        return "Where clause try to reference non existing column";
    }
}
