package space.cubicworld.core.database.nocache;

import lombok.Builder;
import lombok.Data;
import space.cubicworld.core.database.CoreList;

@Data
@Builder
public class CoreListImpl implements CoreList {
    private final int id;
    private final String name;
}