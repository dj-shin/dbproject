package marodb.predicate;

import marodb.relation.Column;
import marodb.type.Value;

import java.util.LinkedHashMap;

/**
 * Created by LastOne on 2016-11-19.
 */
public interface Predicate {
    @Override
    String toString();

    ThreeValuedLogic eval(LinkedHashMap<Column, Value> record);
}
