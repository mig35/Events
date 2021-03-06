package com.azoft.events;

public class EventCallback {

    static EventCallback started(final Event event) {
        return new EventCallback(event, null, null, Status.STARTED);
    }

    static EventCallback result(final Event event, final Object[] result) {
        return new EventCallback(event, result, null, Status.RESULT);
    }

    static EventCallback error(final Event event, final Throwable error) {
        return new EventCallback(event, null, error, Status.ERROR);
    }

    static EventCallback finished(final Event event) {
        return new EventCallback(event, null, null, Status.FINISHED);
    }

    private final Event event;
    private final int id;
    private final Object[] result;
    private final Throwable error;
    private final Status status;

    private boolean isErrorHandled;

    private EventCallback(final Event event, final Object[] result, final Throwable error, final Status status) {
        this.id = event.getId();
        this.result = result;
        this.event = event;
        this.error = error;
        this.status = status;
    }

    public Event getEvent() {
        return event;
    }

    public int getId() {
        return id;
    }

    public <T> T getResult() {
        return getResult(0);
    }

    @SuppressWarnings("unchecked")
    public <T> T getResult(final int index) {
        return result == null || result.length <= index ? null : (T) result[index];
    }

    public Status getStatus() {
        return status;
    }

    public Throwable getError() {
        return error;
    }

    public boolean isStarted() {
        return status == Status.STARTED;
    }

    public boolean isResult() {
        return status == Status.RESULT;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    public boolean isFinished() {
        return status == Status.FINISHED;
    }


    public void markErrorAsHandled() {
        if (status != Status.ERROR) {
            throw new RuntimeException("Cannot markErrorAsHandled for non-ERROR callbacks");
        }
        isErrorHandled = true;
    }

    public boolean isErrorHandled() {
        if (status != Status.ERROR) {
            throw new RuntimeException("Method isErrorHandled does not make sense for non-ERROR callbacks");
        }
        return isErrorHandled;
    }

    public enum Status {
        STARTED, RESULT, ERROR, FINISHED
    }
}