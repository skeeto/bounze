package bounze;

import java.util.Observable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.val;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;

public class Game extends Observable {

    private static final int FPS = 30;
    private static final int V_ITERATIONS = 8;
    private static final int P_ITERATIONS = 3;

    public static final int WIDTH = 64;
    public static final int HEIGHT = 48;

    private static final float BALL_RADIUS = 1.5f;
    private static final float BALL_DENSITY = 1f;
    private static final float BALL_FRICTION = 0f;
    private static final float BALL_RESTITUTION = 0.85f;
    private static final float BALL_DAMPING = 0.5f;
    private static final float BALL_CUTOFF = 3.0f;

    @Getter
    private final World world;

    private final ScheduledExecutorService exec
        = Executors.newSingleThreadScheduledExecutor();

    @Getter
    private final Body ball;

    public Game() {
        world = new World(new Vec2(0, 0), false);

        val top = new PolygonShape();
        top.setAsEdge(new Vec2(0, 0), new Vec2(WIDTH, 0));
        world.createBody(new BodyDef()).createFixture(top, 0f);

        val bottom = new PolygonShape();
        bottom.setAsEdge(new Vec2(0, HEIGHT), new Vec2(WIDTH, HEIGHT));
        world.createBody(new BodyDef()).createFixture(bottom, 0f);

        val left = new PolygonShape();
        left.setAsEdge(new Vec2(0, 0), new Vec2(0, HEIGHT));
        world.createBody(new BodyDef()).createFixture(left, 0f);

        val right = new PolygonShape();
        right.setAsEdge(new Vec2(WIDTH, 0), new Vec2(WIDTH, HEIGHT));
        world.createBody(new BodyDef()).createFixture(right, 0f);

        val ballshape = new CircleShape();
        ballshape.m_radius = BALL_RADIUS;
        val ballbody = new BodyDef();
        ballbody.position = new Vec2(WIDTH / 2, HEIGHT / 2);
        ballbody.type = BodyType.DYNAMIC;
        ballbody.linearDamping = BALL_DAMPING;
        val ballfix = new FixtureDef();
        ballfix.shape = ballshape;
        ballfix.density = BALL_DENSITY;
        ballfix.friction = BALL_FRICTION;
        ballfix.restitution = BALL_RESTITUTION;
        ball = world.createBody(ballbody);
        ball.createFixture(ballfix);

        ball.setLinearVelocity(new Vec2(50f, 20f));

        exec.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    world.step(1f / FPS, V_ITERATIONS, P_ITERATIONS);
                    if (ball.getLinearVelocity().length() < BALL_CUTOFF) {
                        ball.setLinearVelocity(new Vec2(0, 0));
                    }
                    setChanged();
                    notifyObservers();
                }
            }, 0L, (long) (1000.0 / FPS), TimeUnit.MILLISECONDS);
    }
}
