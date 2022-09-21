package gitlet;


import java.io.File;
import java.io.IOException;
import java.util.*;

import static gitlet.Utils.*;


/**
 * Represents a gitlet repository.
 * <p>
 * does at a high level.
 *
 * @author Spike
 */
public class Repository {
    /**
     *
     * <p>
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */
    /**
     * The current working directory.
     */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /**
     * The .gitlet directory.
     */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File BRANCH = join(GITLET_DIR, "branch");
    public static final File COMMITS = join(GITLET_DIR, "commits");
    public static final File BLOBS = join(GITLET_DIR, "blobs");
    public static final File STAGES = join(GITLET_DIR, "stages");
    public static final File HEAD = join(GITLET_DIR, "head");


    public void init() {
        if (GITLET_DIR.exists()) {
            message("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        } else {
            GITLET_DIR.mkdir();
            BRANCH.mkdir();
            BLOBS.mkdir();
            COMMITS.mkdir();
            try {
                STAGES.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                HEAD.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            StagingArea stage = new StagingArea();
            writeObject(STAGES, stage);
            writeContents(HEAD, "master");
            Commit initialCommit = new Commit();
            File currentBranch = join(BRANCH, getHEAD());
            writeObject(currentBranch, initialCommit);
            File commitFile = join(COMMITS, initialCommit.getID());
            writeObject(commitFile, initialCommit);
        }
    }

    public void add(String file) {
        File toAddFile = join(CWD, file);
        if (toAddFile.exists()) {
            String blob = readContentsAsString(toAddFile);
            String blobID = sha1(blob);
            Commit currentCommit = getCurrentCommit();
            StagingArea stage = getStage();
            if (currentCommit.getTree().containsKey(file)
                    && currentCommit.getTree().get(file).equals(blobID)) {
                if (stage.getAddition().containsKey(file)) {
                    stage.getAddition().remove(file);
                }
            } else {
                stage.getAddition().put(file, blobID);
                writeContents(join(BLOBS, blobID), blob);
            }
            if (stage.getRemoval().contains(file)) {
                stage.getRemoval().remove(file);
            }
            writeObject(STAGES, stage);
        } else {
            message("File does not exist");
            System.exit(0);
        }
    }

    public void rm(String file) {
        Commit currentCommit = getCurrentCommit();
        StagingArea stage = getStage();
        if (stage.getAddition().containsKey(file)) {
            stage.getAddition().remove(file);
            writeObject(STAGES, stage);
        } else if (currentCommit.getTree().containsKey(file)) {
            stage.getRemoval().add(file);
            stage.getAddition().remove(file);
            File toRemove = join(CWD, file);
            if (toRemove.exists()) {
                toRemove.delete();
            }
            writeObject(STAGES, stage);
        } else {
            message("No reason to remove the file.");
            System.exit(0);
        }
    }

    public void commit(String msg) {
        StagingArea stage = getStage();
        if (!stage.isEmpty()) {
            Commit currentCommit = getCurrentCommit();
            String parentID = currentCommit.getID();
            HashMap<String, String> newTree = currentCommit.getTree();
            if (!stage.getAddition().isEmpty()) {
                Set<String> files = stage.getAddition().keySet();
                for (String file : files) {
                    String blob = stage.getAddition().get(file);
                    newTree.put(file, blob);
                }
            }
            if (!stage.getRemoval().isEmpty()) {
                for (String file : stage.getRemoval()) {
                    newTree.remove(file);
                }
            }
            Commit newCommit = new Commit(msg, newTree, parentID);
            File commitFile = join(COMMITS, newCommit.getID());
            writeObject(commitFile, newCommit);
            File currentBranch = join(BRANCH, getHEAD());
            writeObject(currentBranch, newCommit);
            clearStage();
        } else {
            message("No changes added to the commit.");
            System.exit(0);
        }
    }

    public void log() {
        Commit curr = getCurrentCommit();
        while (true) {
            System.out.println("===");
            System.out.println("commit " + curr.getID());
            System.out.println("Date: " + curr.getDate());
            System.out.println(curr.getMessage());
            System.out.println();
            if (curr.getParentID() != null) {
                curr = readObject(join(COMMITS, curr.getParentID()), Commit.class);
            } else {
                break;
            }
        }
    }

    public void globallog() {
        List<String> files = plainFilenamesIn(COMMITS);
        if (files != null) {
            for (String file : files) {
                Commit curr = readObject(join(COMMITS, file), Commit.class);
                System.out.println("===");
                System.out.println("commit " + curr.getID());
                System.out.println("Date: " + curr.getDate());
                System.out.println(curr.getMessage());
                System.out.println();
            }
        }
    }

    public void find(String msg) {
        List<String> files = plainFilenamesIn(COMMITS);
        int count = 0;
        for (String file : files) {
            Commit commit = readObject(join(COMMITS, file), Commit.class);
            if (commit.getMessage().equals(msg)) {
                System.out.println(commit.getID());
                count++;
            }
        }
        if (count == 0) {
            message("Found no commit with that message.");
            System.exit(0);
        }
    }

    public void status() {
        if (GITLET_DIR.exists()) {
            StagingArea stage = getStage();
            List<String> branches = plainFilenamesIn(BRANCH);
            Collections.sort(branches);
            System.out.println("=== Branches ===");
            for (String branch : branches) {
                if (branch.equals(getHEAD())) {
                    System.out.println("*" + branch);
                } else {
                    System.out.println(branch);
                }
            }
            System.out.println("\n=== Staged Files ===");
            for (String stageFile : stage.getAddition().keySet()) {
                System.out.println(stageFile);
            }
            System.out.println("\n=== Removed Files ===");
            for (String removeFile : stage.getRemoval()) {
                System.out.println(removeFile);
            }
            Commit curr = getCurrentCommit();
            HashMap<String, String> tree = curr.getTree();
            TreeMap<String, String> addition = (TreeMap<String, String>) stage.getAddition();
            TreeSet<String> removal = (TreeSet<String>) stage.getRemoval();
            List<String> files = plainFilenamesIn(CWD);
            System.out.println("\n=== Modifications Not Staged For Commit ===");
            Set<String> untrackFiles = new TreeSet<String>();
            for (String file : files) {
                if (!tree.containsKey(file)
                        && !addition.containsKey(file)) {
                    untrackFiles.add(file);
                }
                if (removal.contains(file)) {
                    untrackFiles.add(file);
                }
            }
            System.out.println("\n=== Untracked Files ===");
            for (String untrackFile : untrackFiles) {
                System.out.println(untrackFile);
            }
            System.out.println();
        } else {
            message("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
    }

    public void checkout1(String file) {
        HashMap<String, String> tree = getCurrentCommit().getTree();
        if (tree.containsKey(file)) {
            String blob = readContentsAsString(join(BLOBS, tree.get(file)));
            writeContents(join(CWD, file), blob);
        } else {
            message("File does not exist in that commit.");
            System.exit(0);
        }
    }

    public void checkout2(String id, String file) {
        File target = join(COMMITS, id);
        if (target.exists()) {
            HashMap<String, String> tree = readObject(target, Commit.class).getTree();
            if (tree.containsKey(file)) {
                String blob = readContentsAsString(join(BLOBS, tree.get(file)));
                writeContents(join(CWD, file), blob);
            } else {
                message("File does not exist in that commit.");
                System.exit(0);
            }
        } else {
            message("No commit with that id exists.");
            System.exit(0);
        }
    }

    public void checkout3(String branch) {
        if (branch.equals(getHEAD())) {
            message("No need to checkout the current branch.");
            System.exit(0);
        }
        File branchFile = join(BRANCH, branch);
        if (branchFile.exists()) {
            Set<String> trackFiles = getCurrentCommit().getTree().keySet();
            Set<String> files = readObject(branchFile, Commit.class).getTree().keySet();
            List<String> workingDir = plainFilenamesIn(CWD);
            String branchID = readObject(branchFile, Commit.class).getID();
            for (String file : files) {
                if (!trackFiles.contains(file) && workingDir.contains(file)) {
                    message("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    System.exit(0);
                } else {
                    checkout2(branchID, file);
                }
            }
            for (String trackFile : trackFiles) {
                if (!files.contains(trackFile)) {
                    join(CWD, trackFile).delete();
                }
            }
            writeContents(HEAD, branch);
            clearStage();
        } else {
            message("No such branch exists.");
            System.exit(0);
        }

    }

    public void checkoutShort(String miniID, String file) {
        String maxID = "";
        for (String id : plainFilenamesIn(COMMITS)) {
            if (id.contains(miniID)) {
                maxID = id;
            }
        }
        checkout2(maxID, file);
    }

    public void branch(String branch) {
        List<String> branches = plainFilenamesIn(BRANCH);
        if (branches.contains(branch)) {
            message(" A branch with that name already exists.");
            System.exit(0);
        } else {
            File newBranch = join(BRANCH, branch);
            Commit curr = getCurrentCommit();
            writeObject(newBranch, curr);
        }
    }

    public void rmBranch(String branch) {
        List<String> branches = plainFilenamesIn(BRANCH);
        if (!branches.contains(branch)) {
            message("A branch with that name does not exist.");
            System.exit(0);
        } else if (getHEAD().equals(branch)) {
            message("Cannot remove the current branch.");
            System.exit(0);
        } else {
            File toDeleteBranch = join(BRANCH, branch);
            toDeleteBranch.delete();
        }
    }

    public void reset(String id) {
        File commit = join(COMMITS, id);
        if (commit.exists()) {
            HashMap<String, String> trackedFiles = getCurrentCommit().getTree();
            HashMap<String, String> files = readObject(commit, Commit.class).getTree();
            List<String> workingDir = plainFilenamesIn(CWD);
            for (String file : files.keySet()) {
                if (!trackedFiles.containsKey(file) && workingDir.contains(file)) {
                    message("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    System.exit(0);
                } else {
                    checkout2(id, file);
                }
            }
            File currBranch = join(BRANCH, getHEAD());
            Commit givenCommit = readObject(commit, Commit.class);
            writeObject(currBranch, givenCommit);
            clearStage();
        } else {
            message("No commit with that id exists.");
            System.exit(0);
        }
    }

    public Commit getSplitPoint(String branch) {
        String id = getCurrentCommit().getID();
        while (id != null) {
            String branchID = getBranch(branch).getID();
            while (branchID != null) {
                if (branchID.equals(id)) {
                    return getCommit(id);
                }
                branchID = getCommit(branchID).getParentID();
            }
            id = getCommit(id).getParentID();
        }
        return null;
    }

    public void merge(String branchName) {
        int conflict = 0;
        StagingArea stage = getStage();
        Commit curr = getCurrentCommit();
        Commit branch = getBranch(branchName);
        if (!stage.isEmpty()) {
            message("You have uncommitted changes.");
            System.exit(0);
        }
        if (branch == null) {
            message("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(getHEAD())) {
            message("Cannot merge a branch with itself.");
            System.exit(0);
        }
        Commit splitPoint = getSplitPoint(branchName);
        if (splitPoint.getID().equals(branch.getID())) {
            message("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (splitPoint.getID().equals(curr.getID())) {
            checkout3(branchName);
            message("Current branch fast-forwarded.");
            System.exit(0);
        }
        HashMap<String, String> prevTree = splitPoint.getTree();
        HashMap<String, String> currTree = curr.getTree();
        HashMap<String, String> branchTree = branch.getTree();
        for (String file : prevTree.keySet()) {
            if (currTree.containsKey(file) && branchTree.containsKey(file)) {
                if (currTree.get(file).equals(prevTree.get(file))
                        && !branchTree.get(file).equals(prevTree.get(file))) {
                    checkout2(branch.getID(), file);
                    stage.getAddition().put(file, branchTree.get(file));
                }
                if (!currTree.get(file).equals(prevTree.get(file))
                        && !branchTree.get(file).equals(prevTree.get(file))
                        && !currTree.get(file).equals(branchTree.get(file))) {
                    conflict++;
                    writeConflict(file, stage, currTree, branchTree);
                }
            }
            if (currTree.containsKey(file) && !branchTree.containsKey(file)) {
                if (currTree.get(file).equals(prevTree.get(file))) {
                    join(CWD, file).delete();
                    stage.getRemoval().add(file);
                } else {
                    conflict++;
                    writeConflict(file, stage, currTree, branchTree);
                }
            }
            if (branchTree.containsKey(file)
                    && !currTree.containsKey(file)
                    && !branchTree.get(file).equals(prevTree.get(file))) {
                conflict++;
                writeConflict(file, stage, currTree, branchTree);
            }
        }
        for (String file : branchTree.keySet()) {
            if (!prevTree.containsKey(file) && !currTree.containsKey(file)) {
                if (join(CWD, file).exists()) {
                    message("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                }
                checkout2(branch.getID(), file);
                stage.getAddition().put(file, branchTree.get(file));
            }
            if (!prevTree.containsKey(file) && currTree.containsKey(file)
                    && !currTree.get(file).equals(branchTree.get(file))) {
                conflict++;
                writeConflict(file, stage, currTree, branchTree);
            }
        }
        writeObject(STAGES, stage);
        commit("Merged " + branchName + " into " + getHEAD() + ".");
        soutConflict(conflict);
    }

    private void soutConflict(int conflict) {
        if (conflict > 0) {
            message("Encountered a merge conflict.");
        }
    }

    private String getConflict(String curr, String branch) {
        String msg = "<<<<<<< HEAD\n";
        if (curr.length() > 0) {
            msg += curr;
        }
        msg += "=======\n";
        if (branch.length() > 0) {
            msg += branch;
        }
        msg += ">>>>>>>\n";
        return msg;
    }

    private void writeConflict(String file, StagingArea stage,
                               HashMap<String, String> currTree,
                               HashMap<String, String> branchTree) {
        String cur = "";
        if (currTree.containsKey(file)) {
            cur = readContentsAsString(join(BLOBS, currTree.get(file)));
        }
        String bran = "";
        if (branchTree.containsKey(file)) {
            bran = readContentsAsString(join(BLOBS, branchTree.get(file)));
        }
        writeContents(join(CWD, file), getConflict(cur, bran));
        stage.getAddition().put(file, getConflict(cur, bran));
    }

    private Commit getCurrentCommit() {
        return readObject(join(BRANCH, getHEAD()), Commit.class);
    }

    private Commit getCommit(String id) {
        return readObject(join(COMMITS, id), Commit.class);
    }

    private StagingArea getStage() {
        return readObject(STAGES, StagingArea.class);
    }

    private void clearStage() {
        StagingArea stage = new StagingArea();
        writeObject(STAGES, stage);
    }

    private String getHEAD() {
        return readContentsAsString(HEAD);
    }

    private Commit getBranch(String branch) {
        File branchFile = join(BRANCH, branch);
        if (branchFile.exists()) {
            return readObject(branchFile, Commit.class);
        } else {
            return null;
        }
    }

}
