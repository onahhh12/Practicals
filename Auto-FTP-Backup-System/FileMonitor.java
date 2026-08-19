import java.io.File;
import java.util.HashMap;

public class FileMonitor {
    private HashMap<String, FileMeta> state = new HashMap <>();

    public FileChange checkForChanges(String folderPath) {

        File folder = new File(folderPath);
        File[] files = folder.listFiles();

        HashMap<String, Boolean> seen = new HashMap<>();

        FileChange change = null;

        for (File file : files) {

            String name = file.getName();
            long lastModified = file.lastModified();
            seen.put(name, true);

            FileMeta meta = state.get(name);

            if (meta == null) {
                state.put(name, new FileMeta(lastModified, 0, false));

                change = new FileChange(name, 0, FileAction.NEW);
                continue;
            }

            if (meta.lastModified != lastModified) {
                meta.lastModified = lastModified;
                meta.version += 1;

                change = new FileChange(name, meta.version, FileAction.MODIFIED);
            }
        }

        for (String name : state.keySet()) {

            if (!seen.containsKey(name)) {
                state.get(name).deleted = true;
                change = new FileChange(name, state.get(name).version, FileAction.DELETED);
            }
        }
        return null;
    }
}