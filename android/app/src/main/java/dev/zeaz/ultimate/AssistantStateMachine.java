package dev.zeaz.ultimate;

/** Deterministic lifecycle for user-visible assistant work. */
public final class AssistantStateMachine {
    public enum State {
        INITIALIZING, DISCONNECTED, IDLE, LISTENING, TRANSCRIBING,
        THINKING, STREAMING, SPEAKING, ERROR, PAUSED
    }

    public enum Event {
        INITIALIZATION_COMPLETE, CONNECTION_ESTABLISHED, CONNECTION_LOST,
        LISTENING_STARTED, TRANSCRIPTION_STARTED, TRANSCRIPTION_COMPLETED,
        REQUEST_STARTED, STREAM_STARTED, RESPONSE_COMPLETED,
        SPEECH_STARTED, SPEECH_COMPLETED, FAILURE, RETRY, CANCEL, PAUSE, RESUME
    }

    private State state = State.INITIALIZING;
    private State pausedState;

    public State state() {
        return state;
    }

    /** Applies an event, returning false and preserving state when the event is illegal. */
    public boolean transition(Event event) {
        if (event == null) return false;
        if (state == State.PAUSED) {
            if (event == Event.RESUME) {
                state = pausedState;
                pausedState = null;
                return true;
            }
            if (event == Event.PAUSE) return false;
            State next = nextState(pausedState, event);
            if (next == null) return false;
            pausedState = next;
            return true;
        }
        if (event == Event.PAUSE) {
            if (state == State.INITIALIZING) return false;
            pausedState = state;
            state = State.PAUSED;
            return true;
        }
        State next = nextState(state, event);
        if (next == null) return false;
        state = next;
        return true;
    }

    private static State nextState(State current, Event event) {
        if (event == Event.FAILURE && current != State.INITIALIZING && current != State.ERROR) {
            return State.ERROR;
        }
        switch (current) {
            case INITIALIZING:
                return event == Event.INITIALIZATION_COMPLETE ? State.DISCONNECTED : null;
            case DISCONNECTED:
                if (event == Event.CONNECTION_ESTABLISHED) return State.IDLE;
                if (event == Event.LISTENING_STARTED) return State.LISTENING;
                if (event == Event.REQUEST_STARTED) return State.THINKING;
                if (event == Event.SPEECH_STARTED) return State.SPEAKING;
                if (event == Event.RETRY) return State.DISCONNECTED;
                return null;
            case IDLE:
                if (event == Event.CONNECTION_LOST) return State.DISCONNECTED;
                if (event == Event.LISTENING_STARTED) return State.LISTENING;
                if (event == Event.REQUEST_STARTED) return State.THINKING;
                if (event == Event.SPEECH_STARTED) return State.SPEAKING;
                return null;
            case LISTENING:
                if (event == Event.TRANSCRIPTION_STARTED) return State.TRANSCRIBING;
                if (event == Event.CANCEL) return State.IDLE;
                return null;
            case TRANSCRIBING:
                if (event == Event.TRANSCRIPTION_COMPLETED || event == Event.CANCEL) return State.IDLE;
                return null;
            case THINKING:
                if (event == Event.STREAM_STARTED) return State.STREAMING;
                if (event == Event.RESPONSE_COMPLETED || event == Event.CANCEL) return State.IDLE;
                return null;
            case STREAMING:
                if (event == Event.RESPONSE_COMPLETED || event == Event.CANCEL) return State.IDLE;
                return null;
            case SPEAKING:
                if (event == Event.SPEECH_COMPLETED || event == Event.CANCEL) return State.IDLE;
                return null;
            case ERROR:
                if (event == Event.RETRY) return State.DISCONNECTED;
                if (event == Event.CANCEL) return State.IDLE;
                return null;
            default:
                return null;
        }
    }
}
