package marodb.type;

/**
 * Created by LastOne on 2016-10-30.
 */
public class NullType implements DataType {
    @Override
    public String getType() {
        return NULLTYPE;
    }

    @Override
    public String toString() {
        return NULLTYPE;
    }
}

