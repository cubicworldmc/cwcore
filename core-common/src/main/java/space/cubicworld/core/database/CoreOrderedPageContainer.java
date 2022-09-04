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
        Page<T> previousPage = binarySearch(previous);
        Page<T> nextPage = binarySearch(current);
    }

    private Page<T> binarySearch(int value) {
        int leftIndex = 0;
        int rightIndex = pages.size();
        int index = pages.size() / 2;
        while (leftIndex <= index && rightIndex >= index) {
            Page<T> current = pages.get(index);
            int left = getWeight(current.start.value);
            int right = getWeight(current.end.value);
            if (left <= value && right >= value) return current;
            if (left > value) {
                rightIndex = index;
                index /= 2;
            }
            else {
                leftIndex = index;
                index += index / 2;
            }
        }
        return null;
    }

    protected abstract void fetch(Consumer<T> valueConsumer, int pageNumber);

    protected abstract int getWeight(T value);

}
