

public class Process {
    public static final int REQUEST_CREATION_TIME_MS = 7;
    public static final int REQUEST_PROCESSING_TIME_MS = 7;
    private final Processor processor;
    private final File file;
    private final boolean readOnly;
    private final Controller controller;
    private boolean canCreateRequests = true;
    private State state = new State.CreatingRequest(1);
    private int lastRequestedSectorNumber;

    private int createdRequestsCounter = 0;

    private final RequestStyle requestStyle;

    public Process(Processor processor, File file, boolean readOnly, Controller controller) {
        this.processor = processor;
        this.file = file;
        this.readOnly = readOnly;
        this.controller = controller;
        this.lastRequestedSectorNumber = file.getBlocks()[0];

        requestStyle = (file.getType() == File.Type.LARGE && Main.Random.nextBoolean())
                ? RequestStyle.SEQUENTIAL
                : RequestStyle.RANDOM;
    }

    public void tick() {
        switch (state.getType()) {
            case CREATING_QUERY:
                State.CreatingRequest creatingRequestState = (State.CreatingRequest) state;
                int progress = creatingRequestState.getProgress();
                if (progress == REQUEST_CREATION_TIME_MS) {

                    Query.Type requestType = (readOnly)
                            ? Query.Type.READ
                            : (Main.Random.nextBoolean()) ? Query.Type.READ : Query.Type.WRITE;

                    int sectorToBeRequested = (requestStyle == RequestStyle.RANDOM)
                            ? file.getBlocks()[Main.Random.nextInt(file.getBlocks().length)]
                            : file.getBlocks()[(lastRequestedSectorNumber + 1) % file.getBlocks().length];

                    lastRequestedSectorNumber = sectorToBeRequested;
                    Query query = new Query(requestType, sectorToBeRequested, this);

                    state = new State.CreatedRequest(query);
                } else {
                    state = new State.CreatingRequest(progress + 1);
                }
                break;

            case CREATED_QUERY:
                if (!canCreateRequests) {
                    return;
                }

                State.CreatedRequest createdRequestState = (State.CreatedRequest) state;

                try {
                    controller.addRequestToQueue(createdRequestState.getRequest());
                    createdRequestsCounter++;
                    if (createdRequestState.getRequest().getType() == Query.Type.READ) {
                        state = new State.Blocked();
                    } else {
                        state = new State.CreatingRequest(1);
                    }
                } catch (QuerySelectionAlgorithm.QueueFullException exception) {
                    processor.switchContext();
                }
                break;

            case BLOCKED_QUERY:
                throw new IllegalStateException("Can not tick Blocked process!");

            case PROCESSING_QUERY:
                State.ProcessingRequest processingRequestState = (State.ProcessingRequest) state;
                progress = processingRequestState.getProgress();
                state = (progress == REQUEST_PROCESSING_TIME_MS)
                        ? new State.CreatingRequest(1)
                        : new State.ProcessingRequest(progress + 1);
                break;
        }
    }

    public void deliverRequestResult(Query query) {
        if (query.getType() == Query.Type.READ) {
            state = new State.ProcessingRequest(1);
        }
    }

    public void setCanCreateRequests(boolean canCreateRequests) {
        this.canCreateRequests = canCreateRequests;
    }

    public int getCreatedRequestsCounter() {
        return createdRequestsCounter;
    }

    public void resetCreatedRequestsCounter() {
        createdRequestsCounter = 0;
    }

    public Boolean isBlocked() {
        return this.state.type == StateType.BLOCKED_QUERY;
    }

    @Override
    public String toString() {
        return "Process{" + "\n" +
                "\tfile=" + file + "\n" +
                "\treadOnly=" + readOnly + "\n" +
                "\trequestStyle=" + requestStyle + "\n" +
                '}';
    }

    public enum RequestStyle {
        RANDOM,
        SEQUENTIAL
    }

    public enum StateType {
        CREATING_QUERY,
        CREATED_QUERY,
        BLOCKED_QUERY,
        PROCESSING_QUERY
    }

    public static abstract class State {
        private final StateType type;

        public State(StateType type) {
            this.type = type;
        }

        public StateType getType() {
            return type;
        }

        public static class CreatingRequest extends State {
            private final int progress;

            public CreatingRequest(int progress) {
                super(StateType.CREATING_QUERY);
                this.progress = progress;
            }

            public int getProgress() {
                return progress;
            }
        }

        public static class CreatedRequest extends State {
            private final Query query;

            public CreatedRequest(Query query) {
                super(StateType.CREATED_QUERY);
                this.query = query;
            }

            public Query getRequest() {
                return query;
            }
        }

        public static class Blocked extends State {
            public Blocked() {
                super(StateType.BLOCKED_QUERY);
            }
        }

        public static class ProcessingRequest extends State {
            private final int progress;

            public ProcessingRequest(int progress) {
                super(StateType.PROCESSING_QUERY);
                this.progress = progress;
            }

            public int getProgress() {
                return progress;
            }
        }
    }
}
