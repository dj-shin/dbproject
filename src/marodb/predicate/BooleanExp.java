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

    /**
     * List of boolean terms to join with OR operations
     * @param orList
     */
    public BooleanExp(ArrayList<BooleanTerm> orList) {
        this.orList = orList;
    }

    /**
     * Initialize with single boolean term
     * @param bterm
     */
    public BooleanExp(BooleanTerm bterm) {
        orList = new ArrayList<BooleanTerm>();
        orList.add(bterm);
    }

    /**
     * Append to head of boolean terms list
     * @param bterm
     */
    public void addFront(BooleanTerm bterm) {
        orList.add(0, bterm);
    }

    @Override
    public String toString() {
        String result = "[OR](" + orList.get(0).toString() + ")";
        for (int i = 1; i < orList.size(); i++) {
            result += " or (" + orList.get(i).toString() + ")";
        }
        return result;
    }

    /**
     * Evaluate predicate on given record
     * @param record
     * @return
     */
    public ThreeValuedLogic eval(LinkedHashMap<Column, Value> record) {
        ThreeValuedLogic result = new ThreeValuedLogic("FALSE");
        // False is identity value on OR operation
        for (BooleanTerm bTerm : orList) {
            result = result.or(bTerm.eval(record));
        }
        return result;
    }
}
