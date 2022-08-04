package space.cubicworld.core.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import net.kyori.adventure.text.format.TextColor;

import java.util.UUID;

@Data
@Entity
@Table(
        name = "players",
        uniqueConstraints =
                @UniqueConstraint(columnNames = "name")
)
@SuperBuilder
@NoArgsConstructor
public class CorePlayer {

    @Id
    @Column(name = "uuid")
    private UUID uuid;

    @Column(name = "name")
    private String name;

    @Column(name = "reputation")
    private int reputation;

    @Column(name = "global_color")
    private int globalColor = -1;

    public void setGlobalColor(TextColor color) {
        this.globalColor = color == null ? -1 : color.value();
    }

    public TextColor getGlobalColor() {
        return globalColor == -1 ? null : TextColor.color(globalColor);
    }

}
