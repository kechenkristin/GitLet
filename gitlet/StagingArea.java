package gitlet;


import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class StagingArea implements Serializable {
    private Map<String, String> addition;
    private Set<String> removal;

    public StagingArea() {
        addition = new TreeMap<String, String>();
        removal = new TreeSet<>();
    }

    public void clear() {
        addition.clear();
        removal.clear();
    }

    public Map<String, String> getAddition() {
        return addition;
    }

    public Set<String> getRemoval() {
        return removal;
    }

    public boolean isEmpty() {
        return addition.isEmpty() && removal.isEmpty();
    }

}
