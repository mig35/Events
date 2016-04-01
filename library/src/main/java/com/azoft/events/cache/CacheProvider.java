package com.azoft.events.cache;

import com.azoft.events.Event;

public interface CacheProvider {

    boolean loadFromCache(Event event) throws Exception;

    void saveToCache(Event event, Object result) throws Exception;

}
