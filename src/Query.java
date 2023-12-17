public class Query {
    private final Type type;
    private final int sectorNumber;
    private final Process process;

    public Query(Type type, int sectorNumber, Process process) {
        this.type = type;
        this.sectorNumber = sectorNumber;
        this.process = process;
    }

    public Type getType() {
        return type;
    }

    public int getTrackNumber() {
        return sectorNumber / Main.SECTORS_PER_TRACK;
    }

    public Process getProcess() {
        return process;
    }

    public enum Type {
        READ, WRITE
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Query query = (Query) o;

        if (sectorNumber != query.sectorNumber) return false;
        if (type != query.type) return false;
        return process.equals(query.process);
    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + sectorNumber;
        result = 31 * result + process.hashCode();
        return result;
    }
}
