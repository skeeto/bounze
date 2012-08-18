package bounze;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jbox2d.common.Vec2;

@Data
@RequiredArgsConstructor
public class Score {

    private final Vec2 position;

    private final int score;

    private long deathTick = -1;
}
