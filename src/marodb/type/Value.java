package marodb.type;

import java.io.Serializable;

/**
 * Created by LastOne on 2016-11-18.
 */
public interface Value extends Serializable {
    DataType getType();

    @Override
    String toString();
}
