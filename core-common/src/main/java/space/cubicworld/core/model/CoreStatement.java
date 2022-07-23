package space.cubicworld.core.model;

import lombok.Builder;
import lombok.RequiredArgsConstructor;
import space.cubicworld.core.database.CoreDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@Builder
@RequiredArgsConstructor
public final class CoreStatement<T> {

    @FunctionalInterface
    public interface SQLRead<T> {
        T read(CoreResult result) throws SQLException;
    }

    @FunctionalInterface
    public interface SQLConsumer<T> {
        void accept(T object) throws SQLException;
    }

    @FunctionalInterface
    public interface SQLFunction<T, E> {
        E apply(T object) throws SQLException;
    }

    private final String sql;
    private final List<Function<T, Object>> parameters;

    @SafeVarargs
    public final <E> E prepareStatement(SQLFunction<PreparedStatement, E> consumer, CoreDatabase database, T... modelInstances) throws SQLException {
        try (Connection connection = database.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            for (T modelInstance : modelInstances) {
                int counter = 1;
                for (Function<T, Object> parameter : parameters) {
                    statement.setObject(counter++, parameter.apply(modelInstance));
                }
                statement.addBatch();
            }
            return consumer.apply(statement);
        }
    }

    public void prepareStatement(SQLConsumer<PreparedStatement> consumer, CoreDatabase database, T... modelInstances) throws SQLException {
        prepareStatement(statement -> {
            consumer.accept(statement);
            return null;
        }, database, modelInstances);
    }

    @SafeVarargs
    public final void update(CoreDatabase database, T... modelInstances) throws SQLException {
        prepareStatement(PreparedStatement::executeLargeBatch, database, modelInstances);
    }

    public <E> List<E> query(CoreDatabase database, SQLRead<E> read, T modelInstance) throws SQLException {
        return prepareStatement(statement -> {
            try (ResultSet resultSet = statement.executeQuery()) {
                List<E> result = new ArrayList<>();
                CoreResult coreResult = new CoreResult(resultSet);
                while (coreResult.readColumn()) {
                    result.add(read.read(coreResult));
                }
                return result;
            }
        }, database, modelInstance);
    }

    public static class CoreStatementBuilder<T> {

        public CoreStatementBuilder<T> parameters(List<Function<T, Object>> functions) {
            if (parameters == null) parameters = new ArrayList<>();
            parameters.addAll(functions);
            return this;
        }

        public CoreStatementBuilder<T> parameter(Function<T, Object> function) {
            if (parameters == null) parameters = new ArrayList<>();
            parameters.add(function);
            return this;
        }

    }

}
