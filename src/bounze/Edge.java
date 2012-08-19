package bounze;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;

/**
 * Represents a single edge in the game.
 */
@AllArgsConstructor
@RequiredArgsConstructor
public class Edge {

    /** Start point. */
    @Getter
    private final Vec2 a;

    /** End point. */
    @Getter
    private final Vec2 b;

    /** JBox2D Body object that represents thid edge. */
    @Getter
    private final Body body;

    /** Tick number this edge was destroyed. */
    @Getter @Setter
    private long deathTick = -1;
}
