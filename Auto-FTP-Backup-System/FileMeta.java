public class FileMeta {
    long lastModified;
    int version;
    boolean deleted;

    public FileMeta(long lastModified, int version, boolean deleted) {
        this.lastModified = lastModified;
        this.version = version;
        this.deleted = deleted;
    }
    
}
