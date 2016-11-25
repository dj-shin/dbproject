package marodb.type;

/**
 * Created by LastOne on 2016-11-18.
 */
public class CharValue implements Value {
    private String value;

    public CharValue(String value) {
        this.value = value;
    }

    @Override
    public DataType getType() {
        return new CharType(value.length());
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CharValue) {
            CharValue other = (CharValue) obj;
            return value.equals(other.value);
        }
        return false;
    }
}
