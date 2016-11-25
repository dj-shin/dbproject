package marodb.type;

/**
 * Created by LastOne on 2016-11-18.
 */
public class IntValue implements Value {
    private Integer value;

    public IntValue(Integer value) {
        this.value = value;
    }

    @Override
    public DataType getType() {
        return new IntType();
    }

    public Integer getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

