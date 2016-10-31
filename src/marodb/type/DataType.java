package marodb.type;

import java.io.Serializable;

/**
 * Created by LastOne on 2016-10-30.
 */
public interface DataType extends Serializable {
    String INTTYPE = "int";
    String CHARTYPE = "char";
    String DATETYPE = "date";
    String getType();
}
