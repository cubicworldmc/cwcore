package space.cubicworld.core.database;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@RequiredArgsConstructor
public class CoreDatabaseIterator<T> implements AutoCloseable {

    static class WithConnection<T> extends CoreDatabaseIterator<T> {

        private final Connection connection;
        private final PreparedStatement statement;

        public WithConnection(
                Connection connection,
                PreparedStatement statement,
                ResultSet resultSet,
                CoreDatabase.FetchFunction<T> fetchFunction,
                CoreDatabase database
        ) {
            super(resultSet, fetchFunction, database);
            this.connection = connection;
            this.statement = statement;
        }

        @Override
        public void close() throws SQLException {
            super.close();
            if (!statement.isClosed()) statement.close();
            if (!connection.isClosed()) connection.close();
        }
    }

    private static final int STEP = 25;

    private final List<T> fetched = Collections.synchronizedList(new ArrayList<>(STEP));
    private final ResultSet resultSet;
    private final CoreDatabase.FetchFunction<T> fetchFunction;
    private final CoreDatabase database;

    @RequiredArgsConstructor
    private static class RealIterator<T> implements Iterator<T> {

        private final CoreDatabaseIterator<T> databaseIterator;
        private int index = 0;

        @Override
        @SneakyThrows
        public boolean hasNext() {
            return databaseIterator.get(index) != null;
        }

        @Override
        @SneakyThrows
        public T next() {
            return Objects.requireNonNull(databaseIterator.get(index++), "hasNext() is false");
        }
    }

    @Override
    public void close() throws SQLException {
        if (!resultSet.isClosed()) resultSet.close();
    }

    private T get(int index) throws SQLException {
        if (fetched.size() > index) {
            return fetched.get(index);
        }
        if (resultSet.isClosed()) return null;
        for (int i = 0; i < STEP; ++i) {
            T value = fetchFunction.fetch(database, resultSet, 1);
            if (value == null) break;
            fetched.add(value);
        }
        if (fetched.size() <= index) {
            close();
            return null;
        }
        return fetched.get(index);
    }

    public Iterator<T> newIterator() {
        return new RealIterator<>(this);
    }

    public void addObject(T object) {
        fetched.add(object);
    }

}
