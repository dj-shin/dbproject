package marodb.predicate;

import marodb.relation.Column;
import marodb.type.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by LastOne on 2016-11-19.
 */
public class BooleanExp {
    private ArrayList<BooleanTerm> orList;

    public BooleanExp(ArrayList<BooleanTerm> orList) {
        this.orList = orList;
    }

    public BooleanExp(BooleanTerm bterm) {
        orList = new ArrayList<BooleanTerm>();
        orList.add(bterm);
    }

    public void addFront(BooleanTerm bterm) {
        orList.add(0, bterm);
    }

    @Override
    public String toString() {
        String result = "(" + orList.get(0).toString() + ")";
        for (int i = 1; i < orList.size(); i++) {
            result += " or (" + orList.get(i).toString() + ")";
        }
        return result;
    }

    public ThreeValuedLogic eval(LinkedHashMap<Column, Value> record) {
        ThreeValuedLogic result = new ThreeValuedLogic("FALSE");
        for (BooleanTerm bTerm : orList) {
            result = result.or(bTerm.eval(record));
        }
        return result;
    }
}
