import java.util.function.BiConsumer;

class Controller {
    private final HardDrive hardDrive;
    private final QuerySelectionAlgorithm querySelectionAlgorithm;
    private final BiConsumer<Query, Integer> onRequestCompleted;
    private int requestCompletionTime = 0;
    private State state = new State.Idle();

    public Controller(HardDrive hardDrive, QuerySelectionAlgorithm querySelectionAlgorithm, BiConsumer<Query, Integer> onRequestCompleted) {
        this.hardDrive = hardDrive;
        this.querySelectionAlgorithm = querySelectionAlgorithm;
        this.onRequestCompleted = onRequestCompleted;
    }

    public void addRequestToQueue(Query query) throws QuerySelectionAlgorithm.QueueFullException {
        querySelectionAlgorithm.tryAddRequestToQueue(query);
    }

    public void tick() {
        if (state instanceof State.Idle) {
            executeRequestFromQueue();
        } else if (state instanceof State.ExecutingRequest) {
            requestCompletionTime++;
            Query queryUnderExecution = ((State.ExecutingRequest) state).getRequest();
            if (hardDrive.getState().equals(new HardDrive.State.IdleState(queryUnderExecution.getTrackNumber(), true))) {
                hardDrive.doOperationOnCurrentSector();
                onRequestCompleted.accept(queryUnderExecution, requestCompletionTime);

                queryUnderExecution.getProcess().deliverRequestResult(queryUnderExecution);

                executeRequestFromQueue();
            }
        }
    }

    private void executeRequestFromQueue() {
        Query queryToBeExecuted = querySelectionAlgorithm.chooseRequest(hardDrive.getState().getPosition());

        if (queryToBeExecuted != null) {
            hardDrive.moveDriveTo(queryToBeExecuted.getTrackNumber());
            requestCompletionTime = 0;
            state = new State.ExecutingRequest(queryToBeExecuted);
        } else {
            state = new State.Idle();
        }
    }

    public abstract static class State {
        public State() {
        }

        public static class Idle extends State {
            public Idle() {
            }
        }

        public static class ExecutingRequest extends State {
            private final Query query;

            public ExecutingRequest(Query query) {
                this.query = query;
            }

            public Query getRequest() {
                return query;
            }
        }
    }
}
