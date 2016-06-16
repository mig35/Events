package com.azoft.events;

import android.util.Log;

import com.azoft.events.cache.CacheProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

class EventHandler {

    private static final String TAG = EventHandler.class.getSimpleName();

    private final Method method;
    private final Type type;
    private final int eventId;
    private final CacheProvider cache;

    EventHandler(final Method method, final Type type, final int eventId, final CacheProvider cache) {
        this.method = method;
        this.type = type;
        this.eventId = eventId;
        this.cache = cache;
    }

    int getEventId() {
        return eventId;
    }

    Type getType() {
        return type;
    }


    void handle(final Object target, final Object parameter) {
        boolean isCacheUsed = false;
        Throwable error = null;

        if (null != cache) {
            // Asking cache provider for cached result
            try {
                isCacheUsed = cache.loadFromCache((Event) parameter);
                if (Events.isDebug) {
                    Log.d(TAG, "Cached value for event " + Utils.getName(eventId) + " is used = " + isCacheUsed);
                }
            }
            catch (final Throwable e) {
                error = e;
            }
        }

        Object result = null;
        if (!isCacheUsed && null == error) {
            // Calling actual handler method
            try {
                result = method.invoke(target, parameter);
            }
            catch (final InvocationTargetException e) {
                error = e.getTargetException();
            }
            catch (final Exception e) {
                Log.e(TAG, "Cannot handle event " + Utils.getName(eventId) + " using method " + method.getName() + ": " + e.getMessage());
            }
        }

        if (null != cache && null != result) {
            // Storing result in cache
            try {
                cache.saveToCache((Event) parameter, result);
            }
            catch (final Throwable e) {
                error = e;
                result = null; // Ignoring result, cache fix is need
            }
        }

        if (type.isMethod()) {
            final Event event = (Event) parameter;
            if (null == error) {
                if (event.isPostponed) {
                    if (null != result) {
                        EventsDispatcher.sendResult(event, new Object[]{result});
                    }
                } else {
                    if (null == result) {
                        EventsDispatcher.sendFinished(event);
                    } else {
                        EventsDispatcher.sendResultAndFinish(event, new Object[]{result});
                    }
                }
            } else {
                if (event.isPostponed) {
                    EventsDispatcher.sendError(event, error);
                } else {
                    EventsDispatcher.sendErrorAndFinished(event, error);
                }
            }
        } else if (null != error) {
            throw new RuntimeException(error);
        }
    }


    enum Type {
        RECEIVER, METHOD_ASYNC, METHOD_ASYNC_SINGLE, METHOD_UI, CALLBACK;

        boolean isCallback() {
            return CALLBACK == this;
        }

        boolean isMethod() {
            return METHOD_UI == this || isAsync() || isAsyncSingle();
        }

        boolean isAsync() {
            return METHOD_ASYNC == this;
        }

        boolean isAsyncSingle() {
            return METHOD_ASYNC_SINGLE == this;
        }

        boolean isReceiver() {
            return RECEIVER == this;
        }
    }
}