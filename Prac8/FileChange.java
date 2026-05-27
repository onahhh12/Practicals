public class FileChange {
    String fileName;
    int version;
    FileAction action;

    public FileChange(String fileName, int version, FileAction action) {
        this.fileName = fileName;
        this.version = version;
        this.action = action;
    }
    
}
