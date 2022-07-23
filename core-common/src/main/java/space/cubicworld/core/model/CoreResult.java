package space.cubicworld.core.model;

import lombok.RequiredArgsConstructor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

@RequiredArgsConstructor
public final class CoreResult {

    private final ResultSet resultSet;
    private int currentRow = 0;

    @SuppressWarnings("unchecked")
    public <T> T readRow() throws SQLException {
        return (T) resultSet.getObject(++currentRow);
    }

    public <T> CoreResult readRow(Consumer<T> consumer) throws SQLException {
        consumer.accept(readRow());
        return this;
    }

    public boolean readColumn() throws SQLException {
        currentRow = 0;
        return resultSet.next();
    }

}
