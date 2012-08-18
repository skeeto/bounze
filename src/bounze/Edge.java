package bounze;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jbox2d.common.Vec2;

@AllArgsConstructor
@RequiredArgsConstructor
public class Edge {
    @Getter
    private final Vec2 a;

    @Getter
    private final Vec2 b;

    @Getter @Setter
    private long deathTick = -1;
}
