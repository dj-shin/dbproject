package marodb.type;

/**
 * Created by LastOne on 2016-10-30.
 */
public class DateType implements DataType {
    @Override
    public String getType() {
        return DATETYPE;
    }

    @Override
    public String toString() {
        return DATETYPE;
    }
}

