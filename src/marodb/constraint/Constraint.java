package marodb.constraint;

import java.util.ArrayList;

/**
 * Created by LastOne on 2016-10-31.
 */
public interface Constraint {
    String PK = "PK";
    String FK = "FK";

    String getType();
}
