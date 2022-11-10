## About this project
A version-control system that mimics some of the basic features of the popular system Git.


1.The main functionality that Gitlet supports is:
- Commit, saving the contents of entire directories of files.
- Checkout, restoring a version of one or more files or entire commits.
- Log, viewing the history of your backups. 
- branches, maintaining related sequences of commits.
- Merging changes made in one branch into another.

2. Directory Architecture
```
.gitlet 
        -blob
        -commit
        -branch
        -stages
        -HEAD
```

3. Class & Directory created
- blobs: The saved contents of files. Since Gitlet saves many versions of files, a single file might correspond to multiple blobs: each being tracked in a different commit.
- trees: Directory structures mapping names to references to blobs and other trees (subdirectories).
- commits: Combinations of log messages, other metadata (commit date, author, etc.), a reference to a tree, and references to parent commits. The repository also maintains a mapping from branch heads to references to commits, so that certain important commits have symbolic names.
