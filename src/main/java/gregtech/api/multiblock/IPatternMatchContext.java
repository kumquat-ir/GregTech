package gregtech.api.multiblock;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Contains an context used for storing temporary data
 * related to current check and shared between all predicates doing it
 */
public interface IPatternMatchContext {

    void set(String key, Object value);

    <T> Optional<T> get(String key);

    <T> T getOrCreate(String key, Supplier<T> constructor);

    <T> T getOrDefault(String key, T defaultValue);

    void increment(String key, int value); // todo bad (I think)

    <T> T getOrPut(String key, T initialValue);
}
