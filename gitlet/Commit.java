package gitlet;


import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Date;
import java.util.Locale;

import static gitlet.Utils.*;

/**
 * Represents a gitlet commit object.
 * does at a high level.
 *
 * @author Spike
 */
public class Commit implements Serializable {
    /**
     * <p>
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    private String date;
    /**
     * The message of this Commit.
     */
    private String message;
    private String parentID;
    private String ID;
    private HashMap<String, String> tree;

    public Commit() {
        Date now = new Date(0);
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        date = dateFormat.format(now);
        message = "initial commit";
        ID = calID();
        tree = new HashMap<>();
    }

    public Commit(String msg, HashMap<String, String> tree, String parentID) {
        Date now = new Date();
        DateFormat dateFormat = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z", Locale.ENGLISH);
        date = dateFormat.format(now);
        message = msg;
        this.tree = tree;
        this.parentID = parentID;
        ID = calID();
    }

    private String calID() {
        return sha1(serialize(this));
    }

    public String getMessage() {
        return message;
    }

    public String getDate() {
        return date;
    }

    public HashMap<String, String> getTree() {
        return tree;
    }

    public String getID() {
        return ID;
    }

    public String getParentID() {
        return parentID;
    }

}
