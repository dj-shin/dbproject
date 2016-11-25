package marodb.predicate;

import marodb.relation.Column;
import marodb.type.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by LastOne on 2016-11-19.
 */
public class BooleanTerm {
    private ArrayList<BooleanTest> andList;

    public BooleanTerm(ArrayList<BooleanTest> andList) {
        this.andList = andList;
    }

    public BooleanTerm(BooleanTest btest) {
        andList = new ArrayList<BooleanTest>();
        andList.add(btest);
    }
    public void addFront(BooleanTest btest) {
        andList.add(0, btest);
    }

    @Override
    public String toString() {
        String result = "(" + andList.get(0).toString() + ")";
        for (int i = 1; i < andList.size(); i++) {
            result += " and (" + andList.get(i).toString() + ")";
        }
        return result;
    }

    public ThreeValuedLogic eval(LinkedHashMap<Column, Value> record) {
        ThreeValuedLogic result = new ThreeValuedLogic("TRUE");
        for (BooleanTest bTest : andList) {
            result = result.and(bTest.eval(record));
        }
        return result;
    }

}
