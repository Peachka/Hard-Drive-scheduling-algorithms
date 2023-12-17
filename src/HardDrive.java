public class HardDrive {
    private static final int DEFAULT_MOVEMENT_TIME_PER_TRACK_MS = 10;
    private static final int DEFAULT_ROTATIONAL_LATENCY_MS = 8;
    private static final int DEFAULT_MOVEMENT_TIME_BETWEEN_FIRST_AND_OUTER_TRACK_MS = 130;

    private final boolean[][] tracks;
    private final int movementTimePerTrackMs;
    private final int rotationalLatencyMs;
    private final int movementTimeBetweenFirstAndOuterTrack;

    private State state;

    public HardDrive(boolean[][] tracks) {
        this(tracks, DEFAULT_MOVEMENT_TIME_PER_TRACK_MS, DEFAULT_ROTATIONAL_LATENCY_MS, DEFAULT_MOVEMENT_TIME_BETWEEN_FIRST_AND_OUTER_TRACK_MS);
    }

    public HardDrive(boolean[][] tracks, int movementTimePerTrackMs, int rotationalLatencyMs, int movementTimeBetweenFirstAndOuterTrack) {
        this.tracks = tracks;
        this.movementTimePerTrackMs = movementTimePerTrackMs;
        this.rotationalLatencyMs = rotationalLatencyMs;
        this.movementTimeBetweenFirstAndOuterTrack = movementTimeBetweenFirstAndOuterTrack;
        this.state = new State.IdleState(0, false);
    }

    public void moveDriveTo(int targetTrack) {
        state = (state.getPosition() == targetTrack) ?
                new State.WaitingForRotationState(state.getPosition(), 1) :
                new State.MovingState(state.getPosition(), targetTrack, 1);
    }

    public void doOperationOnCurrentSector() {
        if (state instanceof State.IdleState && ((State.IdleState) state).isReady()) {
            state = new State.IdleState(state.position, false);
        } else {
            throw new IllegalStateException("Can not do operation when drive is not ready!");
        }
    }

    public State getState() {
        return state;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < tracks.length; i++) {
            stringBuilder.append(i);
            for (int j = 0; j < tracks[i].length; j++) {
                stringBuilder.append(" ").append(j).append(":").append(tracks[i][j]);
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    public void tick() {
        if (state instanceof State.IdleState && !((State.IdleState) state).isReady()) {
            state = new State.WaitingForRotationState(state.getPosition(), 1);
        } else if (state instanceof State.MovingState movingState) {
            int progress = movingState.getProgress();
            int targetPosition = movingState.getTargetPosition();

            if (state.getPosition() == targetPosition) {
                state = new State.WaitingForRotationState(state.getPosition(), 1);
                return;
            }

            if (progress == movementTimePerTrackMs) {
                if (state.getPosition() > targetPosition) {
                    state = new State.MovingState(state.getPosition() - 1, targetPosition, 1);
                } else {
                    state = new State.MovingState(state.getPosition() + 1, targetPosition, 1);
                }
            } else {
                state = new State.MovingState(state.getPosition(), targetPosition, progress + 1);
            }
        } else if (state instanceof State.WaitingForRotationState waitingState) {
            int progress = waitingState.getProgress();
            if (progress == rotationalLatencyMs) {
                state = new State.IdleState(state.getPosition(), true);
            } else {
                state = new State.WaitingForRotationState(state.getPosition(), progress + 1);
            }
        }
    }

    public static class State {
        private final int position;

        public State(int position) {
            this.position = position;
        }

        public int getPosition() {
            return position;
        }

        public static class IdleState extends State {
            private final boolean ready;

            public IdleState(int position, boolean ready) {
                super(position);
                this.ready = ready;
            }

            public boolean isReady() {
                return ready;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;

                IdleState idleState = (IdleState) o;

                return ready == idleState.ready;
            }

            @Override
            public int hashCode() {
                return (ready ? 1 : 0);
            }
        }

        public static class MovingState extends State {
            private final int targetPosition;
            private final int progress;

            public MovingState(int position, int targetPosition, int progress) {
                super(position);
                this.targetPosition = targetPosition;
                this.progress = progress;
            }

            public int getTargetPosition() {
                return targetPosition;
            }

            public int getProgress() {
                return progress;
            }
        }

        public static class WaitingForRotationState extends State {
            private final int progress;

            public WaitingForRotationState(int position, int progress) {
                super(position);
                this.progress = progress;
            }

            public int getProgress() {
                return progress;
            }
        }
    }
}
