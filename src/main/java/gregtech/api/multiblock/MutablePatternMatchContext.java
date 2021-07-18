package gregtech.api.multiblock;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class MutablePatternMatchContext implements IPatternMatchContext {

    private final Map<String, Object> contextData = new HashMap<>();

    public void resetContext() {
        this.contextData.clear();
    }

    @Override
    public void set(String key, Object value) {
        this.contextData.put(key, value);
    }

    @Override
    public <T> Optional<T> get(String key) {
        Object rawValue = this.contextData.get(key);
        return Optional.ofNullable((T) rawValue);
    }

    @Override
    public <T> T getOrCreate(String key, Supplier<T> constructor) {
        Object rawValue = this.contextData.get(key);
        if (rawValue == null) {
            T value = constructor.get();
            set(key, value);
            return value;
        }
        return (T) rawValue;
    }

    @Override
    public <T> T getOrDefault(String key, T defaultValue) {
        Object rawValue = this.contextData.get(key);
        return rawValue == null ? defaultValue : (T) rawValue;
    }

    @Override
    public void increment(String key, int value) {
        set(key, getOrDefault(key, 0) + value);
    }

    @Override
    public <T> T getOrPut(String key, T initialValue) {
        Object rawValue = this.contextData.get(key);
        if (rawValue == null) {
            set(key, initialValue);
            return initialValue;
        }
        return (T) rawValue;
    }
}
