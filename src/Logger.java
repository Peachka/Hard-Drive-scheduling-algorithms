import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Logger {
    private static final Map<Entity, FileWriter> fileWriters = new HashMap<>();

    public static void init(String directory, String prefix) {

        for (Entity entity : Entity.values()) {
            try {
                fileWriters.put(entity, new FileWriter(directory + "/" + prefix + "_" + entity.name() + ".txt"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void write(Entity entity, String text) {
        try {
            fileWriters.get(entity).write(text);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void close() {
        for (FileWriter writer : fileWriters.values()) {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    enum Entity {
        HARD_DRIVE_STATE,
        DRIVE_POSITION,
        REQUEST_EXECUTION_TIME,
        MAX_RPS,
        SUMMARY,
        PRETTY_HARD_DRIVE_STATE,
        REQUEST_TRACK_NUMBER,
        CREATED_REQUESTS_COUNTER
    }
}
