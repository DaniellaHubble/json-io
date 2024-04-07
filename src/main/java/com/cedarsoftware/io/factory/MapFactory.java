package com.cedarsoftware.io.factory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cedarsoftware.io.JsonIoException;
import com.cedarsoftware.io.JsonObject;
import com.cedarsoftware.io.JsonReader;
import com.cedarsoftware.io.Resolver;

/**
 * Use to create new instances of Map interfaces (needed for empty Maps).  Used
 * internally to handle Map, SortedMap when they are within parameterized types.
 */
@Deprecated // Not really, indicating it is creating, but not LOADING.
public class MapFactory implements JsonReader.ClassFactory
{
    /**
     * @param c        Map interface that was requested for instantiation.
     * @param jObj     JsonObject
     * @param resolver
     * @return a concrete Map type.
     */
    @Override
    public Object newInstance(Class<?> c, JsonObject jObj, Resolver resolver)
    {
        if (SortedMap.class.isAssignableFrom(c))
        {
            return new TreeMap<>();
        }
        else if (Map.class.isAssignableFrom(c))
        {
            return new LinkedHashMap<>();
        }
        throw new JsonIoException("MapFactory handed Class for which it was not expecting: " + c.getName());
    }
}