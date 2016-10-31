package marodb.util;

import java.io.Serializable;

/**
 * Created by LastOne on 2016-10-31.
 */
public class Pair<S,T> implements Serializable {
    private S first;
    private T second;

    public Pair(S first, T second) {
        this.first = first;
        this.second = second;
    }

    public S first() {
        return first;
    }

    public T second() {
        return second;
    }

    public void setFirst(S first) {
        this.first = first;
    }

    public void setSecond(T second) {
        this.second = second;
    }
}
