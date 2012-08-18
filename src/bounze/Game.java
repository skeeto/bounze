package bounze;

import java.util.HashSet;
import java.util.Observable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.val;
import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.Contact;

public class Game extends Observable implements ContactListener {

    private static final int FPS = 30;
    private static final int V_ITERATIONS = 8;
    private static final int P_ITERATIONS = 3;

    public static final int WIDTH = 64;
    public static final int HEIGHT = 48;

    private static final float BALL_RADIUS = 1.25f;
    private static final float BALL_DENSITY = 1f;
    private static final float BALL_FRICTION = 0f;
    private static final float BALL_RESTITUTION = 0.85f;
    private static final float BALL_DAMPING = 0.5f;
    private static final float BALL_CUTOFF = 3.0f;
    private static final float BALL_VELOCITY = 60.0f;

    private static final Random RNG = new Random();

    private final Set<Fixture> edges = new HashSet<>();

    @Getter
    private final World world;

    private final ScheduledExecutorService exec
        = Executors.newSingleThreadScheduledExecutor();

    @Getter
    private final Body ball;
    private final Fixture ballFixture;

    private final Set<Body> dead = new HashSet<>();

    public Game() {
        world = new World(new Vec2(0, 0), false);

        val top = new PolygonShape();
        top.setAsEdge(new Vec2(0, 0), new Vec2(WIDTH, 0));
        edges.add(world.createBody(new BodyDef()).createFixture(top, 0f));

        val bottom = new PolygonShape();
        bottom.setAsEdge(new Vec2(0, HEIGHT), new Vec2(WIDTH, HEIGHT));
        edges.add(world.createBody(new BodyDef()).createFixture(bottom, 0f));

        val left = new PolygonShape();
        left.setAsEdge(new Vec2(0, 0), new Vec2(0, HEIGHT));
        edges.add(world.createBody(new BodyDef()).createFixture(left, 0f));

        val right = new PolygonShape();
        right.setAsEdge(new Vec2(WIDTH, 0), new Vec2(WIDTH, HEIGHT));
        edges.add(world.createBody(new BodyDef()).createFixture(right, 0f));

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
        ballFixture = ball.createFixture(ballfix);

        generateLevel(8);

        world.setContactListener(this);
        exec.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    world.step(1f / FPS, V_ITERATIONS, P_ITERATIONS);
                    for (Body b : dead) {
                        world.destroyBody(b);
                    }
                    dead.clear();
                    if (ballStopped()) {
                        ball.setLinearVelocity(new Vec2(0, 0));
                    }
                    setChanged();
                    notifyObservers();
                }
            }, 0L, (long) (1000.0 / FPS), TimeUnit.MILLISECONDS);
    }

    private void generateLevel(int n) {
        for (int i = 0; i < n; i++) {
            val shape = new PolygonShape();
            shape.setAsEdge(randomPosition(), randomPosition());
            world.createBody(new BodyDef()).createFixture(shape, 0f);
        }
    }

    private Vec2 randomPosition() {
        return new Vec2(RNG.nextFloat() * WIDTH, RNG.nextFloat() * HEIGHT);
    }

    public boolean ballStopped() {
        return ball.getLinearVelocity().length() < BALL_CUTOFF;
    }

    public void shoot(Vec2 dir) {
        dir.normalize();
        ball.setLinearVelocity(dir.mul(BALL_VELOCITY));
    }

    @Override
    public void beginContact(Contact contact) {
    }

    @Override
    public void endContact(Contact contact) {
        Fixture a = contact.getFixtureA();
        Fixture b = contact.getFixtureB();
        if (ballFixture != a && !edges.contains(a)) {
            dead.add(a.getBody());
        }
        if (ballFixture != b && !edges.contains(b)) {
            dead.add(b.getBody());
        }
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    }
}
