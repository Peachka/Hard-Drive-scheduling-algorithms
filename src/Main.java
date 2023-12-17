import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Main {

    public static Random Random = new Random(1303);
    public static int PROCESS_QUANTITY = 10;
    public static double NEIGHBORING_SECTOR_WRITE_PROBABILITY = 0.3;
    public static int TRACK_QUANTITY = 500;
    public static int SECTORS_PER_TRACK = 100;
    public static int QUEUE_SIZE = 20;
    public static int SIMULATION_REQUESTS = 100_000;
    public static int time = 0;

    public static void main(String[] args) {
        System.out.println("Choose your algorithm:");
        System.out.println("1. FCFS");
        System.out.println("2. SSTF");
        System.out.println("3. F_LOOK");

        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();
        QuerySelectionAlgorithm policy = createPolicy(choice);
        System.out.println("Chosen algorithm - " + policy.getClass().getSimpleName());

        System.out.println("Now choose maximum QPS:");
        int maxRPS = scanner.nextInt();


        execute(policy, maxRPS);
    }

    public static QuerySelectionAlgorithm createPolicy(int choice) {
        return switch (choice) {
            case 1 -> new FCFS(QUEUE_SIZE);
            case 2 -> new SSTF(QUEUE_SIZE);
            case 3 -> new F_LOOK(QUEUE_SIZE);
            default -> throw new IllegalArgumentException("Invalid input!");
        };
    }

    public static void execute(QuerySelectionAlgorithm policy, int maxRPS) {
        String loggerDirectory = "C:\\Users\\Anastasia\\Desktop\\Labs\\PP\\kursova_java\\src\\output";
        String loggerPrefix = policy.getClass().getSimpleName() + "_" + maxRPS + "maxRPS";
        Logger.init(loggerDirectory, loggerPrefix);

        final int[] completedRequestsCounter = {0};

        int[] requestCompletionTimes = new int[SIMULATION_REQUESTS];

        int currentFileBlock = 0;

        boolean[][] hardDriveTracks = new boolean[TRACK_QUANTITY][SECTORS_PER_TRACK];
        HardDrive hardDrive = new HardDrive(hardDriveTracks);
        Controller controller = new Controller(hardDrive, policy, (request, requestCompletionTime) -> {
            completedRequestsCounter[0]++;
            requestCompletionTimes[completedRequestsCounter[0] - 1] = requestCompletionTime;

            Logger.write(Logger.Entity.REQUEST_TRACK_NUMBER, time + "\t" + request.getTrackNumber() + "\n");
        });

        List<Process> processes = new ArrayList<>(PROCESS_QUANTITY);

        Processor processor = new Processor(processes, Processor.DEFAULT_TIME_QUANTUM_MS, maxRPS);

        for (int i = 0; i < PROCESS_QUANTITY; i++) {
            File.Type fileType = File.Type.values()[Random.nextInt(Query.Type.values().length + 1)];
            int fileSize = switch (fileType) {
                case SMALL -> Random.nextInt(1, 11);
                case MEDIUM -> Random.nextInt(11, 151);
                case LARGE -> Random.nextInt(151, 501);
            };


            int[] fileBlocks = new int[fileSize];
            for (int j = 0; j < fileSize; j++) {
                boolean successfulWriteToNeighboringBlock = Random.nextDouble() < NEIGHBORING_SECTOR_WRITE_PROBABILITY;

                int fileBlock;
                if (successfulWriteToNeighboringBlock) {
                    fileBlock = currentFileBlock;
                } else {
                    fileBlock = currentFileBlock + 1;
                }

                if (successfulWriteToNeighboringBlock) {
                    currentFileBlock++;
                } else {
                    currentFileBlock += 2;
                }

                hardDriveTracks[fileBlock / SECTORS_PER_TRACK][fileBlock % SECTORS_PER_TRACK] = true;
                fileBlocks[j] = fileBlock;
            }

            boolean fileIsReadOnly = Random.nextBoolean();
            processes.add(new Process(processor, new File(fileType, fileSize, fileBlocks), fileIsReadOnly, controller));
        }

        Logger.write(Logger.Entity.HARD_DRIVE_STATE, hardDrive.toString());
        Logger.write(Logger.Entity.PRETTY_HARD_DRIVE_STATE, getPrettyHardDriveState(hardDriveTracks));

        for (int i = 0; i < processes.size(); i++) {
            Logger.write(Logger.Entity.SUMMARY, "Process №" + i + "\n");
            Logger.write(Logger.Entity.SUMMARY, processes.get(i).toString() + "\n\n");
        }

        while (completedRequestsCounter[0] < SIMULATION_REQUESTS) {
            processor.tick();
            controller.tick();
            hardDrive.tick();

            time++;

            Logger.write(Logger.Entity.DRIVE_POSITION, hardDrive.getState().getPosition() + "\n");
        }

        for (int requestCompletionTime : requestCompletionTimes) {
            Logger.write(Logger.Entity.REQUEST_EXECUTION_TIME, requestCompletionTime + "\n");
        }

        System.out.println("Total completed requests: " + completedRequestsCounter[0]);
        System.out.println("Simulation time: " + processor.getTime() + " ms");
        System.out.println("Average RPS: " + (float) completedRequestsCounter[0] / (processor.getTime() / 1000.0));

        Logger.write(Logger.Entity.SUMMARY, "\n\n\n");

        Logger.write(Logger.Entity.SUMMARY, "Total completed requests: " + completedRequestsCounter[0] + "\n");
        Logger.write(Logger.Entity.SUMMARY, "Simulation time: " + processor.getTime() + " ms\n");
        Logger.write(Logger.Entity.SUMMARY, "Average RPS: " + (float) completedRequestsCounter[0] / (processor.getTime() / 1000.0) + "\n");

        Logger.close();
    }

    public static String getPrettyHardDriveState(boolean[][] hardDriveTracks) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int trackNumber = 0; trackNumber < TRACK_QUANTITY; trackNumber++) {
            stringBuilder.append(trackNumber).append("\t");
            for (int sectorNumber = 0; sectorNumber < SECTORS_PER_TRACK; sectorNumber++) {
                if (hardDriveTracks[trackNumber][sectorNumber]) {
                    stringBuilder.append("■");
                } else {
                    stringBuilder.append("□");
                }
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    public static int getExponentiallyDistributedInt(int max) {
        if (!(max > 1)) {
            throw new IllegalArgumentException("max should be greater than 1");
        }

        double rawResult = (max - ((Math.log(1 - Random.nextDouble()) / (-3.0)) * max));
        int result = (int) rawResult;

        if (result < 1) {
            return getExponentiallyDistributedInt(max);
        } else {
            return result;
        }
    }
}