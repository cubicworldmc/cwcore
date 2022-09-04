package space.cubicworld.core.database;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@RequiredArgsConstructor
public abstract class CoreOrderedPageContainer<T> {

    @Data
    private static class LinkedObject<T> {
        @NotNull
        private final T value;
        @Nullable
        private LinkedObject<T> left;
        @Nullable
        private LinkedObject<T> right;
    }

    @Data
    private static class Page<T> {

        private final int order;

        private LinkedObject<T> start;
        private LinkedObject<T> end;
        private int size;

    }

    private final List<Page<T>> pages = new ArrayList<>();

    protected final int maximumPageSize;

    public void update(T object, int previous, int current) {
    }

    protected abstract void fetch(Consumer<T> valueConsumer, int pageNumber);

    protected abstract int getWeight(T value);

}
