package com.azoft.events;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SuppressWarnings({"ProhibitedExceptionThrown", "OverlyComplexClass"})
final class EventsDispatcher {

    private static final String TAG = EventsDispatcher.class.getSimpleName();

    @SuppressWarnings("CollectionDeclaredAsConcreteClass")
    private static final LinkedList<EventReceiver> HANDLERS = new LinkedList<EventReceiver>();

    private static final Queue<QueuedEvent> QUEUE = new LinkedList<QueuedEvent>();

    private static final ExecutorService ASYNC_SINGLE_EXECUTOR = Executors.newSingleThreadExecutor();
    private static final ExecutorService ASYNC_EXECUTOR = Executors.newCachedThreadPool();

    private static final SparseArray<List<Event>> STARTED_EVENTS = new SparseArray<List<Event>>();
    private static final SparseArray<List<Event>> SINGLE_EVENTS = new SparseArray<List<Event>>();

    private static final long MAX_TIME_IN_MAIN_THREAD = 10L;
    private static final long MESSAGE_DELAY = 10L;

    private static final int MSG_POST_EVENT = 0;
    private static final int MSG_POST_CALLBACK = 1;
    private static final int MSG_POST_CALLBACKS = 2;
    private static final int MSG_CANCEL_EVENT = 3;
    private static final int MSG_DISPATCH = 4;

    @SuppressWarnings("OverlyComplexAnonymousInnerClass")
    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(final Message msg) {
            //noinspection SwitchStatementWithoutDefaultBranch
            switch (msg.what) {
                case MSG_POST_EVENT:
                    postEventInternal((Event) msg.obj);
                    break;
                case MSG_POST_CALLBACK:
                    postCallbackInternal((EventCallback) msg.obj);
                    break;
                case MSG_POST_CALLBACKS:
                    for (final EventCallback eventCallback : (EventCallback[]) msg.obj) {
                        postCallbackInternal(eventCallback);
                    }
                    break;
                case MSG_CANCEL_EVENT:
                    cancelEventInternal((Event) msg.obj);
                    break;
                case MSG_DISPATCH:
                    dispatchEventsInternal();
                    break;
            }
        }
    };

    private static EventsErrorHandler sEventsErrorHandler = EventsErrorHandler.DEFAULT;

    private EventsDispatcher() {
    }

    @SuppressWarnings("SuspiciousGetterSetter")
    static void setEventsErrorHandler(final EventsErrorHandler handler) {
        sEventsErrorHandler = handler;
    }

    @SuppressWarnings({"MethodWithMoreThanThreeNegations", "OverlyComplexMethod", "OverlyLongMethod", "VariableNotUsedInsideIf"})
    static void register(final Object target, final boolean keepStrongReference, final String targetId, final Boolean markAsResumed) {
        if (null == target) {
            throw new NullPointerException("Target cannot be null");
        }
        if (keepStrongReference && null != targetId) {
            throw new IllegalArgumentException("strong reference with targetId is not allowed");
        }

        EventReceiver eventReceiver = null;
        for (final EventReceiver receiver : HANDLERS) {
            if (Objects.equalsTargets(targetId, receiver.getTarget())) {
                throw new RuntimeException("Events receiver " + Utils.getClassName(target) + " already registered");
            }
            if (Objects.equalsTargetIds(targetId, receiver.getTargetId())) {
                if (null != eventReceiver) {
                    throw new IllegalStateException("double receivers with same targetId found");
                }
                if (!receiver.isInPause()) {
                    throw new IllegalStateException("old receiver was not in pause and try to register again!");
                }
                eventReceiver = receiver;
            }
        }

        final EventReceiver receiver;
        if (null == eventReceiver) {
            receiver = new EventReceiver(target, targetId, keepStrongReference);
            HANDLERS.addFirst(receiver);
            if (Events.isDebug) {
                Log.d(TAG, "Found new receiver: " + Utils.getClassName(target));
            }
        } else {
            receiver = eventReceiver;
            receiver.setTarget(target);
            if (Events.isDebug) {
                Log.d(TAG, "Found old receiver: " + Utils.getClassName(target));
            }
        }
        if (null != markAsResumed) {
            if (markAsResumed) {
                receiver.markAsResumed();
                if (Events.isDebug) {
                    Log.d(TAG, "Receiver marked as resumed!: " + Utils.getClassName(target));
                }
            } else {
                receiver.markAsPaused();
            }
        }
        notifyStickyEvents(receiver);
    }

    static void pause(final Object target, final String targetId) {
        if (null == target || null == targetId) {
            throw new NullPointerException("Target and targetId cannot be null");
        }

        boolean notFound = true;

        for (final EventReceiver receiver : HANDLERS) {
            if (Objects.equalsTargets(receiver.getTarget(), target) || Objects.equalsTargetIds(targetId, receiver.getTargetId())) {
                if (receiver.isInPause()) {
                    // already in pause. nothing to do
                    return;
                }
                receiver.markAsPaused();
                notFound = false;
                break;
            }
        }

        if (notFound) {
            throw new RuntimeException("Events receiver " + Utils.getClassName(target) + " was not registered");
        }

        if (Events.isDebug) {
            Log.d(TAG, "Events receiver paused: " + Utils.getClassName(target));
        }
    }

    static void resume(final Object target) {
        if (null == target) {
            throw new NullPointerException("Target cannot be null");
        }

        EventReceiver targetReceiver = null;
        for (final EventReceiver receiver : HANDLERS) {
            if (Objects.equals(receiver.getTarget(), target)) {
                if (!receiver.isInPause()) {
                    // already in resume. nothing to do
                    return;
                }
                receiver.markAsResumed();
                targetReceiver = receiver;
                break;
            }
        }

        if (null == targetReceiver) {
            throw new RuntimeException("Events receiver " + Utils.getClassName(target) + " was not registered");
        }
        dispatchEvents();

        if (Events.isDebug) {
            Log.d(TAG, "Events receiver resumed: " + Utils.getClassName(target));
        }
    }

    static void unregister(final Object target) {
        if (null == target) {
            throw new NullPointerException("Target cannot be null");
        }

        boolean isUnregistered = false;

        for (final Iterator<EventReceiver> iterator = HANDLERS.iterator(); iterator.hasNext(); ) {
            final EventReceiver receiver = iterator.next();
            if (Objects.equals(receiver.getTarget(), target)) {
                receiver.markAsUnregistered();
                iterator.remove();
                isUnregistered = true;
                break;
            }
        }

        if (!isUnregistered) {
            throw new RuntimeException("Events receiver " + Utils.getClassName(target) + " was not registered");
        } else if (Events.isDebug) {
            Log.d(TAG, "Events receiver unregistered: " + Utils.getClassName(target));
        }
    }

    /**
     * This method should always be called from UI thread
     */
    static void postEventTo(final Event event, final Object receiver) {
        if (null == receiver) {
            throw new NullPointerException("receiver can't be null");
        }
        if (!Objects.equals(Looper.getMainLooper(), Looper.myLooper())) {
            throw new IllegalStateException("This method can only be called on MainThread");
        }
        EventReceiver singleReceiver = null;
        for (final EventReceiver eventReceiver : HANDLERS) {
            if (Objects.equals(receiver, eventReceiver.getTarget())) {
                singleReceiver = eventReceiver;
                break;
            }
        }
        if (null == singleReceiver) {
            throw new IllegalArgumentException("Receiver wasn't found. Have you registered it before?");
        }
        event.eventReceiver = singleReceiver;
        postEvent(event);
    }

    static void postEvent(final Event event) {
        // Asking main thread to handle this event
        MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_POST_EVENT, event), MESSAGE_DELAY);
    }

    /**
     * This method will always be called from UI thread
     */
    @SuppressWarnings("VariableNotUsedInsideIf")
    private static void postEventInternal(final Event event) {
        final int eventId = event.getId();

        if (Events.isDebug) {
            Log.d(TAG, "Internal event post: " + Utils.getName(eventId));
        }
        if (event.isSingleEvent) {
            List<Event> singleEventsWithId = SINGLE_EVENTS.get(eventId);
            if (null != singleEventsWithId) {
                for (final Event singleEvent : singleEventsWithId) {
                    if (isSameEvent(event, singleEvent)) {
                        if (null == event.eventReceiver && null == singleEvent.eventReceiver) {
                            // this is the same event for all receivers. so we should skip it.
                            return;
                        }
                        //noinspection StatementWithEmptyBody
                        if (null != event.eventReceiver && null != singleEvent.eventReceiver) {

                            // this is single event. we should check if this event has the same receiver
                            if (Objects.equalsTargets(event.eventReceiver.getTarget(), singleEvent.eventReceiver.getTarget()) ||
                                    Objects.equalsTargetIds(event.eventReceiver.getTargetId(), singleEvent.eventReceiver.getTargetId())) {
                                // receiver is the same. so skip
                                return;
                            }
                            // this is an other receiver, so we should start this event again
                        } else {
                            // one event is for all item and an other is for single... we can't skip this event
                        }
                        break;
                    }
                }
            }
            if (null == singleEventsWithId) {
                singleEventsWithId = new ArrayList<Event>();
                SINGLE_EVENTS.put(eventId, singleEventsWithId);
            }
            singleEventsWithId.add(event);
        }

        for (final EventReceiver receiver : HANDLERS) {
            if (null == receiver.getMethods()) {
                continue;
            }

            for (final EventHandler method : receiver.getMethods()) {
                if (method.getEventId() != eventId || method.getType().isCallback()) {
                    continue;
                }
                if (method.getType().isReceiver() && null != event.eventReceiver) {
                    if (!(Objects.equalsTargets(event.eventReceiver.getTarget(), receiver.getTarget()) ||
                            Objects.equalsTargetIds(event.eventReceiver.getTargetId(), receiver.getTargetId()))) {
                        continue;
                    }
                }

                if (null == event.handlerType) {
                    event.handlerType = method.getType();
                } else if (event.handlerType.isMethod()) {
                    throw new RuntimeException("Event of type " + event.handlerType + " can have only one handler");
                } else if (method.getType().isMethod()) {
                    throw new RuntimeException("Event of type " + event.handlerType + " can't have handlers of type " + method.getType());
                }

                if (event.isCanceled) {
                    Log.d(TAG, "Canceled event tried to scheduled: " + Utils.getName(eventId) + " / type = " + method.getType());
                } else {
                    if (null != event.handlerType) {
                        if (event.handlerType.isMethod()) {
                            postCallbackInternal(EventCallback.started(event));
                        }
                    }
                    QUEUE.add(QueuedEvent.create(receiver, method, event));

                    if (Events.isDebug) {
                        Log.d(TAG, "Event scheduled: " + Utils.getName(eventId) + " / type = " + method.getType());
                    }
                }
            }
        }

        if (null != event.handlerType) {
            dispatchEvents();
        }
    }

    private static boolean isSameEvent(final Event event, final Event otherEvent) {
        if (event.getId() != otherEvent.getId()) {
            return false;
        }
        final int dataCount = event.getDataCount();
        if (dataCount != otherEvent.getDataCount()) {
            return false;
        }
        for (int i = 0; i < dataCount; ++i) {
            final Object eventDataItem = event.getData(i);
            final Object otherEventDataItem = otherEvent.getData(i);
            if (null == eventDataItem ? null != otherEventDataItem : !eventDataItem.equals(otherEventDataItem)) {
                return false;
            }
        }
        return true;
    }

    private static void postCallback(final EventCallback callback) {
        postCallbacks(callback);
    }

    private static void postCallbacks(final EventCallback... callbacks) {
        if (null == callbacks || 0 == callbacks.length) {
            throw new RuntimeException("Can't send empty callbacks");
        }
        for (final EventCallback callback : callbacks) {
            final EventHandler.Type handlerType = callback.getEvent().handlerType;
            if (null == handlerType || !handlerType.isMethod()) {
                throw new RuntimeException("Cannot sent " + callback.getStatus() + " callback for event of type " + handlerType);
            }
        }

        // Asking main thread to handle this callback
        if (1 == callbacks.length) {
            MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_POST_CALLBACK, callbacks[0]), MESSAGE_DELAY);
        } else {
            MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_POST_CALLBACKS, callbacks), MESSAGE_DELAY);
        }
    }

    /**
     * This method will always be called from UI thread
     */
    private static void postCallbackInternal(final EventCallback callback) {
        final int eventId = callback.getId();

        if (Events.isDebug) {
            Log.d(TAG, "Internal callback post: " + Utils.getName(eventId) + " / status = " + callback.getStatus());
        }

        final Event event = callback.getEvent();
        if (event.isFinished) {
            if (Events.isDebug) {
                Log.d(TAG, "Event " + Utils.getName(eventId) +
                        " was already finished, ignoring " + callback.getStatus() + " callback");
            }
            return;
        }

        if (callback.isStarted()) {
            // Saving started event
            List<Event> events = STARTED_EVENTS.get(eventId);
            if (null == events) {
                STARTED_EVENTS.put(eventId, events = new LinkedList<Event>());
            }
            events.add(event);
        } else if (callback.isFinished()) {
            // Removing finished event
            STARTED_EVENTS.get(eventId).remove(event);

            if (event.isSingleEvent) {
                final List<Event> singleEventsWithId = SINGLE_EVENTS.get(eventId);
                if (null != singleEventsWithId) {
                    singleEventsWithId.remove(event);
                }
            }

            if (event.isFinished) {
                return;
            } else {
                event.isFinished = true;
            }
        }

        for (final EventReceiver receiver : HANDLERS) {
            if (null == receiver.getMethods()) {
                continue;
            }

            for (final EventHandler method : receiver.getMethods()) {
                if (method.getEventId() != eventId || !method.getType().isCallback()) {
                    continue;
                }
                if (null != event.eventReceiver && !Objects.equals(event.eventReceiver, receiver)) {
                    continue;
                }

                QUEUE.add(QueuedEvent.create(receiver, method, callback));

                if (Events.isDebug) {
                    Log.d(TAG, "Callback scheduled: " + Utils.getName(eventId));
                }
            }
        }

        if (callback.isError()) {
            QUEUE.add(QueuedEvent.createErrorHandler(callback));
        }

        dispatchEvents();
    }

    static void sendResult(final Event event, final Object[] result) {
        postCallback(EventCallback.result(event, result));
    }

    /**
     * Will add result and finish in one loop. needed to prevent wrong result, started, finish order
     */
    static void sendResultAndFinish(final Event event, final Object[] result) {
        postCallbacks(EventCallback.result(event, result), EventCallback.finished(event));
    }

    static void sendError(final Event event, final Throwable error) {
        postCallback(EventCallback.error(event, error));
    }

    static void sendFinished(final Event event) {
        postCallback(EventCallback.finished(event));
    }

    static void sendErrorAndFinished(final Event event, final Throwable error) {
        postCallbacks(EventCallback.error(event, error), EventCallback.finished(event));
    }

    private static void notifyStickyEvents(final EventReceiver receiver) {
        if (null == receiver.getMethods()) {
            return;
        }

        for (final EventHandler method : receiver.getMethods()) {
            if (!method.getType().isCallback()) {
                continue;
            }

            final int eventId = method.getEventId();

            final List<Event> events = STARTED_EVENTS.get(eventId);
            if (null != events) {
                for (final Event event : events) {
                    if (null != event.eventReceiver && !Objects.equals(event.eventReceiver, receiver)) {
                        continue;
                    }

                    QUEUE.add(QueuedEvent.create(receiver, method, EventCallback.started(event)));
                    if (Events.isDebug) {
                        Log.d(TAG, "Callback of type STARTED is resent: " + Utils.getName(eventId));
                    }
                }
            }
        }

        dispatchEvents();
    }

    static void cancelEvent(final Event event) {
        MAIN_THREAD.sendMessageDelayed(MAIN_THREAD.obtainMessage(MSG_CANCEL_EVENT, event), MESSAGE_DELAY);
    }

    /**
     * This method will always be called from UI thread
     */
    private static void cancelEventInternal(final Event event) {
        if (!event.isCanceled && !event.isFinished) {
            for (final Iterator<QueuedEvent> iterator = QUEUE.iterator(); iterator.hasNext(); ) {
                if (Objects.equals(iterator.next().event, event)) {
                    iterator.remove();
                }
            }

            if (Events.isDebug) {
                Log.d(TAG, "Canceling event: " + Utils.getName(event.getId()));
            }
            event.isCanceled = true;
            if (event.isSingleEvent && null == event.handlerType) {
                // some single events are not processed
                return;
            }
            postCallback(EventCallback.finished(event));
        }

        // Note, that we have a gap between cancelEvent method and cancelEventInternal method where
        // isCanceled is actually set to true. So some callbacks including FINISHED can be sent during this gap.
        // So we should have mechanism to prevent repeated FINISHED events - see flag Event#isFinished.
    }

    private static void dispatchEvents() {
        if (!MAIN_THREAD.hasMessages(MSG_DISPATCH)) {
            MAIN_THREAD.sendEmptyMessageDelayed(MSG_DISPATCH, MESSAGE_DELAY);
        }
    }

    /**
     * This method will always be called from UI thread
     */
    private static void dispatchEventsInternal() {
        if (Events.isDebug) {
            Log.d(TAG, "Dispatching started");
        }

        final long started = SystemClock.uptimeMillis();

        for (final Iterator<QueuedEvent> iterator = QUEUE.iterator(); iterator.hasNext(); ) {
            final QueuedEvent queuedEvent = iterator.next();

            if (queuedEvent.isErrorHandling) {
                // error handling don't have receiver, but other should have
                final EventCallback callback = (EventCallback) queuedEvent.event;
                if (!callback.isErrorHandled() && null != sEventsErrorHandler) {
                    sEventsErrorHandler.onError(callback);
                }
                iterator.remove();
            } else {
                if (queuedEvent.receiver.isUnregistered()) {
                    iterator.remove();
                    continue;
                }
                if (queuedEvent.receiver.isInPause()) {
                    continue;
                }
                iterator.remove();

                if (!queuedEvent.receiver.isUnregistered()) {
                    if (Events.isDebug) {
                        Log.d(TAG, "Dispatching: " + queuedEvent.method.getType() + " event = " + Utils.getName(queuedEvent.method.getEventId()));
                    }

                    final EventHandler.Type methodType = queuedEvent.method.getType();
                    if (methodType.isAsync()) {
                        ASYNC_EXECUTOR.execute(new AsyncRunnable(queuedEvent));
                    } else if (methodType.isAsyncSingle()) {
                        ASYNC_SINGLE_EXECUTOR.execute(new AsyncRunnable(queuedEvent));
                    } else {
                        executeQueuedEvent(queuedEvent);
                    }
                }

                if (MAX_TIME_IN_MAIN_THREAD < SystemClock.uptimeMillis() - started) {
                    if (Events.isDebug) {
                        Log.d(TAG, "Dispatching: time in main thread = " + (SystemClock.uptimeMillis() - started) +
                                "ms, scheduling next dispatch cycle");
                    }
                    dispatchEvents();
                    return;
                }
            }
        }
    }

    private static void executeQueuedEvent(final QueuedEvent queuedEvent) {
        if (queuedEvent.receiver.isUnregistered() || queuedEvent.receiver.isInPause()) {
            Log.d(TAG, "Dispatching: executeQueuedEvent = isUnregistered or isInPause");
            return; // Receiver was unregistered or paused
        }
        final Object target = queuedEvent.receiver.getTarget();

        if (null == target) {
            Log.d(TAG, "Dispatching: executeQueuedEvent = target == null");
/*
            todo check if this code is needed

            Log.e(TAG, "Event receiver " + queuedEvent.receiver.getTargetClass().getName() + " was not correctly unregistered");
            queuedEvent.receiver.markAsUnregistered();
*/
            return;
        }

        queuedEvent.method.handle(target, queuedEvent.event);
    }

    private static class QueuedEvent {

        final EventReceiver receiver;
        final EventHandler method;
        final Object event;

        final boolean isErrorHandling;

        private QueuedEvent(final EventReceiver receiver, final EventHandler method, final Object event, final boolean isErrorHandling) {
            this.receiver = receiver;
            this.method = method;
            this.event = event;
            this.isErrorHandling = isErrorHandling;
        }

        static QueuedEvent create(final EventReceiver receiver, final EventHandler method, final Object event) {
            return new QueuedEvent(receiver, method, event, false);
        }

        static QueuedEvent createErrorHandler(final EventCallback callback) {
            return new QueuedEvent(null, null, callback, true);
        }
    }

    private static class AsyncRunnable implements Runnable {

        private final QueuedEvent queuedEvent;

        AsyncRunnable(final QueuedEvent queuedEvent) {
            this.queuedEvent = queuedEvent;
        }

        @Override
        public void run() {
            executeQueuedEvent(queuedEvent);
        }
    }
}