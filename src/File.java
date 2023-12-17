import java.util.Arrays;

public class File {
    private final Type type;
    private final int size;
    private final int[] blocks;

    public File(Type type, int size, int[] blocks) {
        this.type = type;
        this.size = size;
        this.blocks = Arrays.copyOf(blocks, blocks.length);
    }

    public Type getType() {
        return type;
    }

    public int[] getBlocks() {
        return Arrays.copyOf(blocks, blocks.length);
    }

    @Override
    public String toString() {
        return "File{" +
                "type=" + type +
                ", size=" + size +
                ", blocks=" + Arrays.toString(blocks) +
                '}';
    }

    public enum Type {
        SMALL, // Невеликі файли (від 1 до 10 блоків)
        MEDIUM, // Середні файли (від 11 до 150 блоків)
        LARGE // Великі файли (від 151 до 500 блоків)
    }
}