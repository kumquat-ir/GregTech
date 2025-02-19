package gregtech.api.recipes.recipeproperties;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public final class EmptyRecipePropertyStorage implements IRecipePropertyStorage {

    public static final EmptyRecipePropertyStorage INSTANCE = new EmptyRecipePropertyStorage();

    private EmptyRecipePropertyStorage() {

    }

    @Override
    public boolean store(RecipeProperty<?> recipeProperty, Object value) {
        return false;
    }

    @Override
    public boolean remove(RecipeProperty<?> recipeProperty) {
        return false;
    }

    @Override
    public void freeze(boolean frozen) {

    }

    @Override
    public IRecipePropertyStorage copy() {
        return null; // Fresh for RecipeBuilder to handle
    }

    @Override
    public int getSize() {
        return 0;
    }

    @Override
    public Set<Map.Entry<RecipeProperty<?>, Object>> getRecipeProperties() {
        return Collections.emptySet();
    }

    @Override
    public <T> T getRecipePropertyValue(RecipeProperty<T> recipeProperty, T defaultValue) {
        return defaultValue;
    }

    @Override
    public boolean hasRecipeProperty(RecipeProperty<?> recipeProperty) {
        return false;
    }

    @Override
    public Set<String> getRecipePropertyKeys() {
        return Collections.emptySet();
    }

    @Override
    public Object getRawRecipePropertyValue(String key) {
        return null;
    }
}
