import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;

public class Processor {

    public static final int DEFAULT_TIME_QUANTUM_MS = 20;

    private final List<Process> processes;
    private final int timeQuantum;
    private final int maxRequestsPerSecond;

    private long time = 0;
    private int currentProcessActiveTime = 0;
    private int currentActiveProcessIndex = 0;
    private int maxRequestsPerThisSecond = 0;
    private final int[] maxRequestsPerProcess;

    public Processor(List<Process> processes, int timeQuantum, int maxRequestsPerSecond) {
        this.processes = processes;
        this.timeQuantum = (timeQuantum <= 0) ? DEFAULT_TIME_QUANTUM_MS : timeQuantum;
        this.maxRequestsPerSecond = maxRequestsPerSecond;

        this.maxRequestsPerProcess = new int[Main.PROCESS_QUANTITY];
        initializeMaxRequestsPerProcess();
    }

    private void initializeMaxRequestsPerProcess() {
        if (maxRequestsPerSecond > 0) {
            maxRequestsPerThisSecond = Main.getExponentiallyDistributedInt(maxRequestsPerSecond);

            int usedRequests = 0;
            int curIndex = 0;

            Arrays.fill(maxRequestsPerProcess, 0);

            while (usedRequests < maxRequestsPerThisSecond) {
                maxRequestsPerProcess[curIndex] += 1;
                usedRequests++;
                curIndex = (curIndex + 1) % maxRequestsPerProcess.length;
            }
        }
    }

    private void resetCreatedRequestsCounter() {
        for (Process process : processes) {
            Logger.write(Logger.Entity.CREATED_REQUESTS_COUNTER, process.getCreatedRequestsCounter() + ", ");
            process.resetCreatedRequestsCounter();
        }
        Logger.write(Logger.Entity.CREATED_REQUESTS_COUNTER, "\n");
    }

    public void tick() {
        if (time % 1_000L == 0L) {
            initializeMaxRequestsPerProcess();
            resetCreatedRequestsCounter();
            Logger.write(Logger.Entity.MAX_RPS, maxRequestsPerThisSecond + "\n");
        }

        Process currentProcess = processes.get(currentActiveProcessIndex);
        if (currentProcess.isBlocked()) {
            Optional<Process> firstUnblockedAfterCurrent = IntStream.range(currentActiveProcessIndex + 1, processes.size())
                    .filter(i -> !processes.get(i).isBlocked())
                    .mapToObj(processes::get)
                    .findFirst();

            if (firstUnblockedAfterCurrent.isPresent()) {
                currentProcess = firstUnblockedAfterCurrent.get();
                currentActiveProcessIndex = processes.indexOf(currentProcess);
            } else {
                Optional<Process> minIndexUnblocked = IntStream.range(0, processes.size())
                        .filter(i -> !processes.get(i).isBlocked())
                        .mapToObj(processes::get)
                        .min(Comparator.comparingInt(processes::indexOf));

                currentProcess = minIndexUnblocked.orElse(null);
                if (currentProcess != null) {
                    currentActiveProcessIndex = processes.indexOf(currentProcess);
                }
            }
        }

        if (currentProcess != null) {
            currentProcess.setCanCreateRequests(currentProcess.getCreatedRequestsCounter() < maxRequestsPerProcess[currentActiveProcessIndex]);
            currentProcess.tick();
            currentProcessActiveTime++;
        }

        time++;

        if (currentProcessActiveTime == timeQuantum) {
            currentActiveProcessIndex = (currentActiveProcessIndex + 1) % Main.PROCESS_QUANTITY;
            currentProcessActiveTime = 0;
        }
    }

    public void switchContext() {
        currentActiveProcessIndex = (currentActiveProcessIndex + 1) % Main.PROCESS_QUANTITY;
        currentProcessActiveTime = 0;
    }

    public long getTime() {
        return time;
    }
}

