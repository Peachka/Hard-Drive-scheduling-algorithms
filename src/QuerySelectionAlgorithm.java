import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

interface QuerySelectionAlgorithm {
    void tryAddRequestToQueue(Query query) throws QueueFullException;
    Query chooseRequest(int currentDrivePosition);
    class QueueFullException extends Exception {
    }
}


class FCFS implements QuerySelectionAlgorithm {
    private final int maxQueueSize;
    private final List<Query> queue;
    public FCFS(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        this.queue = new ArrayList<>();
    }

    @Override
    public void tryAddRequestToQueue(Query query) throws QueueFullException {
        if (queue.size() == maxQueueSize) {
            throw new QueueFullException();
        }
        queue.add(query);
    }

    @Override
    public Query chooseRequest(int currentDrivePosition) {
        return (queue.isEmpty()) ? null : queue.remove(0);
    }
}

class SSTF implements QuerySelectionAlgorithm {
    private final int maxQueueSize;
    private final List<Query> queue;

    public SSTF(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        this.queue = new ArrayList<>();
    }

    @Override
    public void tryAddRequestToQueue(Query query) throws QueueFullException {
        if (queue.size() == maxQueueSize) {
            throw new QueueFullException();
        }
        queue.add(query);
    }

    @Override
    public Query chooseRequest(int currentDrivePosition) {
        Query closestTrackQuery = queue.stream()
                .min(Comparator.comparingInt(q -> Math.abs(currentDrivePosition - q.getTrackNumber())))
                .orElse(null);

        if (closestTrackQuery != null) {
            queue.remove(closestTrackQuery);
        }

        return closestTrackQuery;
    }
}

class F_LOOK implements QuerySelectionAlgorithm {
    private final int maxQueueSize;
    private QueueState queueState;
    private LOOKDirection lookDirection;

    private final List<Query> firstQueue;
    private final List<Query> secondQueue;

    public F_LOOK(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        this.queueState = QueueState.FIRST_QUEUE_ACTIVE;
        this.lookDirection = LOOKDirection.ASCENDING;
        this.firstQueue = new ArrayList<>();
        this.secondQueue = new ArrayList<>();
    }

    @Override
    public void tryAddRequestToQueue(Query query) throws QueueFullException {
        switch (queueState) {
            case FIRST_QUEUE_ACTIVE -> {
                if (secondQueue.size() == maxQueueSize / 2) {
                    throw new QueueFullException();
                }
                secondQueue.add(query);
            }
            case SECOND_QUEUE_ACTIVE -> {
                if (firstQueue.size() == maxQueueSize / 2) {
                    throw new QueueFullException();
                }
                firstQueue.add(query);
            }
        }
    }

    @Override
    public Query chooseRequest(int currentDrivePosition) {
        switch (queueState) {
            case FIRST_QUEUE_ACTIVE -> {
                if (firstQueue.isEmpty()) {
                    queueState = QueueState.SECOND_QUEUE_ACTIVE;
                    return LOOK(currentDrivePosition, secondQueue);
                }
                return LOOK(currentDrivePosition, firstQueue);
            }
            case SECOND_QUEUE_ACTIVE -> {
                if (secondQueue.isEmpty()) {
                    queueState = QueueState.FIRST_QUEUE_ACTIVE;
                    return LOOK(currentDrivePosition, firstQueue);
                }
                return LOOK(currentDrivePosition, secondQueue);
            }
        }
        return null;
    }

    private Query LOOK(int currentDrivePosition, List<Query> queue) {
        if (queue.isEmpty()) {
            return null;
        }

        switch (lookDirection) {
            case ASCENDING -> {
                Query queryWithBiggerTrackNumber = queue.stream()
                        .filter(q -> q.getTrackNumber() >= currentDrivePosition)
                        .min(Comparator.comparingInt(q -> q.getTrackNumber() - currentDrivePosition))
                        .orElse(null);
                if (queryWithBiggerTrackNumber != null) {
                    queue.remove(queryWithBiggerTrackNumber);
                    return queryWithBiggerTrackNumber;
                } else {
                    Query queryToBeExecuted = queue.stream()
                            .max(Comparator.comparingInt(Query::getTrackNumber))
                            .orElse(null);

                    if (queryToBeExecuted != null) {
                        queue.remove(queryToBeExecuted);
                    }

                    lookDirection = LOOKDirection.DESCENDING;
                    return queryToBeExecuted;
                }
            }
            case DESCENDING -> {
                Query queryWithSmallerTrackNumber = queue.stream()
                        .filter(q -> q.getTrackNumber() <= currentDrivePosition)
                        .min(Comparator.comparingInt(q -> currentDrivePosition - q.getTrackNumber()))
                        .orElse(null);
                if (queryWithSmallerTrackNumber != null) {
                    queue.remove(queryWithSmallerTrackNumber);
                    return queryWithSmallerTrackNumber;
                } else {
                    Query queryToBeExecuted = queue.stream()
                            .min(Comparator.comparingInt(Query::getTrackNumber))
                            .orElse(null);

                    if (queryToBeExecuted != null) {
                        queue.remove(queryToBeExecuted);
                    }

                    lookDirection = LOOKDirection.ASCENDING;
                    return queryToBeExecuted;
                }
            }
        }
        return null;
    }

    private enum QueueState {
        FIRST_QUEUE_ACTIVE,
        SECOND_QUEUE_ACTIVE
    }

    private enum LOOKDirection {
        ASCENDING,
        DESCENDING
    }
}