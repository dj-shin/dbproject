package marodb.type;

/**
 * Created by LastOne on 2016-11-18.
 */
public class NullValue implements Value {
    @Override
    public DataType getType() {
        return new NullType();
    }

    @Override
    public String toString() {
        return DataType.NULLTYPE;
    }

    @Override
    public boolean equals(Object obj) {
        return false;
    }
}

