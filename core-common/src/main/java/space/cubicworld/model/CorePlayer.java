package space.cubicworld.model;


import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Entity
@Table(
        name = "PLAYERS",
        uniqueConstraints =
                @UniqueConstraint(columnNames = "NAME")
)
@Builder
public class CorePlayer {

    @Id
    @Column(name = "uuid")
    private final UUID uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "reputation")
    private int reputation;

}
