package org.example;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class Session {
    private final String id;

    private final Map<String, Object> values = new ConcurrentHashMap<>();

    public Session(String id){
        this.id = id;
    }

    public String getId(){
        return id;
    }

    public void set(String key, Object value){
        values.put(key, value);
    }

    public Object get(String key){
        return values.get(key);
    }

    public String getString(String key){
        Object value = values.get(key);
        return value == null ? null : values.toString();
    }

    public void remove(String key){
        values.remove(key);
    }
}
