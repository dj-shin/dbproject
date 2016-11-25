package marodb.exceptions;

/**
 * Created by LastOne on 2016-11-25.
 */
public class WhereAmbiguousReference extends WhereError {
    @Override
    public String getMessage() {
        return "Where clause contains ambiguous reference";
    }
}
