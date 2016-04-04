Events for Android and Java
======================

## Description

This library works in every Java project but has effective tools to support Android projects too.

How to use it with Android:

You can use different event ids: 
1. Android resource id;
2. constant strings.

If you are using android resource ids then set this code in Application class.

    Events.setAppContext(this);

Next step is to register any classes that want to listen for any events.
There are 4 event types:
* AsyncMethod
* UiMethod
* Callback
* Receiver

Event (except) Receiver has this states:

* Start 	- when event was posted.
* Result 	- event result. can be passed multiple times (see Event.postResult method).
* Error 	- state when exception was thrown in any Async or Ui methods.
* Finish 	- terminated state. after this no other events can be passed except start.

You can annotate any method with this event type. But methods should have required signature.

# AsyncMethod

This method should have this signature

    private AnyClass runTask1(final Event event) throws Exception

Can be only ONE in all application.
This method will be executed in background thread when somebody started this event.
If you return result from this method this result will be passed to any Callback method for this event id.
If you return null no result will be passed to Callback.

# UiMethod

    private AnyClass runTask1(final Event event) throws Exception

Same as AsyncMethod, but will be executed in UI thread.
Can be only ONE in all application

# Callback

    private void runTask1(final EventCallback callback)

Called on UI thread any time when Event state changes. Support: start, result, error, finish.
Can be any count of the callbacks in application, but only one in class.

# Receiver

    private void runTask1(final EventCallback callback)

Called on UI thread. This is a broadcast element of the event. There can be any count of the receivers with the same key.


# Event creation

For event creation you should user Event.create method (or simple Events.post).
Main features:
* data		- data that will be passed to Event during its execution
* single	- indicates that only one event with the same id and data should be processed at time. event will be skipped only if it is in progress now and has the same target.
* post 	- target of the event to post result to. this means that there is no specific target and any Callback will be called.
* postTo 	- target of the event to post result to. this means that only this instance Callbacks will be called.


# Event state manipulation

* sendResult 	- in any time of AsyncMethod or UiMethod you can call this method to send result.
* postpone 	- if you call this, then event will not be finished after method execution finishes. only call to finish() will do this
* finish 		- terminate event
* cancel 		- now works with problems...


# Annotation parameters

* General annotation parameters:
** value    - constant int value, generally it is android id identifier. notice that in library projects android id identifier can't be used because it is not constant value instead of them you can use "static final int" value or "key" annotation parameter. value can not be 0.
** key      - constant string value. value can not be null or empty string.
* Special annotation parameters:
** singleThreadExecutor - parameter of AsyncMethod. if true then SingleThreadExecutor instance will be used for processing this event. Be aware that there is only one instance of executor for all events.


# Activity and Fragment recreation problem:

To handle all activity and fragment recreations and good work of postTo you should use EventsActivity and EventsFragment classes. Call all there lifecycle methods and be sure that:
* Event that was posted before activity/fragment recreation with postTo (or post) will be routed to the new recreated activity/fragment (and not old one).
* No leaks will be occurred because of good register/unregister calls.
* No event will be triggered after onSaveInstanceState method call (after this call it is not save to perform any operation).
* No event will be triggered after fragment detach (its view destroy).

Call all bellow methods:
* onCreate
* onResume
* onSaveInstanceState
* onDestroyView - fragment only
* onDestroy




#### License ####

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
