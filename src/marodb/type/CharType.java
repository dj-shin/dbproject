package marodb.type;

/**
 * Created by LastOne on 2016-10-30.
 */
public class CharType implements DataType {
    private Integer length;

    @Override
    public String getType() {
        return CHARTYPE + "(" + length.toString() + ")";
    }

    public Integer getLength() {
        return length;
    }

    public CharType (Integer length) {
        this.length = length;
    }

    @Override
    public String toString() {
        return CHARTYPE + "(" + length.toString() + ")";
    }
}
