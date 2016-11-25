package marodb.relation;

import marodb.predicate.BooleanExp;
import marodb.predicate.ThreeValuedLogic;
import marodb.type.Value;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Created by LastOne on 2016-11-24.
 */
public class Relation implements Serializable {
    private ArrayList<LinkedHashMap<Column, Value>> instance;

    public Relation() {
        instance = new ArrayList<LinkedHashMap<Column, Value>>();
    }

    public void addRecord(LinkedHashMap<Column, Value> record) {
        instance.add(record);
    }

    public ArrayList<LinkedHashMap<Column, Value>> getInstance() {
        return instance;
    }

    public Relation projection(ArrayList<Column> columnList) {
        Relation result = new Relation();
        for (LinkedHashMap<Column, Value> record : instance) {
            LinkedHashMap<Column, Value> projectionRecord = new LinkedHashMap<Column, Value>();
            for (Column column : columnList) {
                projectionRecord.put(column, record.get(column));
            }
            result.addRecord(projectionRecord);
        }
        return result;
    }

    public Relation join(Relation other) {
        Relation result = new Relation();

        for (LinkedHashMap<Column, Value> myRecord : instance) {
            for (LinkedHashMap<Column, Value> otherRecord : other.instance) {
                LinkedHashMap<Column, Value> newRecord = new LinkedHashMap<Column, Value>();
                for (Column myColumn : myRecord.keySet()) {
                    newRecord.put(myColumn, myRecord.get(myColumn));
                }
                for (Column otherColumn : otherRecord.keySet()) {
                    newRecord.put(otherColumn, otherRecord.get(otherColumn));
                }
                result.addRecord(newRecord);
            }
        }
        return result;
    }

    public Relation select(BooleanExp predicate) {
        Relation result = new Relation();

        for (LinkedHashMap<Column, Value> record : instance) {
            if (predicate.eval(record).equals(new ThreeValuedLogic("TRUE"))) {
                result.addRecord(record);
            }
        }
        return result;
    }

    public void pprint() {
        if (instance.size() == 0) {
            System.out.println("Empty set");
            return;
        }

        ArrayList<Integer> width = new ArrayList<Integer>();
        int i = 0;
        for (Column column : instance.get(0).keySet()) {
            width.add(column.toString().length() + 1);
        }
        for (i = 0; i < instance.size(); i++) {
            int j = 0;
            for (Column column : instance.get(i).keySet()) {
                if (width.get(j) < instance.get(i).get(column).toString().length() + 1) {
                    width.set(j, instance.get(i).get(column).toString().length() + 1);
                }
                j++;
            }
        }

        for (Integer w : width) {
            System.out.print("+");
            for (i = 0; i < w; i++) {
                System.out.print("-");
            }
        }
        System.out.println("+");
        i = 0;
        for (Column column : instance.get(0).keySet()) {
            System.out.print("|");
            System.out.print(column.toString());
            for (int j = column.toString().length(); j < width.get(i); j++) {
                System.out.print(" ");
            }
            i++;
        }
        System.out.println("|");
        for (Integer w : width) {
            System.out.print("+");
            for (i = 0; i < w; i++) {
                System.out.print("-");
            }
        }
        System.out.println("+");
        for (LinkedHashMap<Column, Value> record : instance) {
            i = 0;
            for (Value value : record.values()) {
                System.out.print("|");
                System.out.print(value.toString());
                for (int j = value.toString().length(); j < width.get(i); j++) {
                    System.out.print(" ");
                }
                i++;
            }
            System.out.println("|");
        }
        for (Integer w : width) {
            System.out.print("+");
            for (i = 0; i < w; i++) {
                System.out.print("-");
            }
        }
        System.out.println("+");
    }
}
