package marodb.type;

/**
 * Created by LastOne on 2016-11-18.
 */
public class DateValue implements Value {
    private String value;

    public DateValue(String value) {
        this.value = value;
    }

    @Override
    public DataType getType() {
        return new DateType();
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}

