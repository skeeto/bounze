package bounze;

import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Observable;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.extern.java.Log;
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

@Log
public class Game extends Observable implements ContactListener {

    public static final int FPS = 30;
    private static final int V_ITERATIONS = 8;
    private static final int P_ITERATIONS = 3;

    public static final int WIDTH = 56;
    public static final int HEIGHT = 36;

    private static final float BALL_RADIUS = 1.25f;
    private static final float BALL_DENSITY = 1f;
    private static final float BALL_FRICTION = 0f;
    private static final float BALL_RESTITUTION = 0.85f;
    private static final float BALL_DAMPING = 0.7f;
    private static final float BALL_CUTOFF = 5.0f;
    private static final float BALL_VELOCITY = 60.0f;

    private static final Random RNG = new Random();

    private static final float MIN_EDGE = Math.max(WIDTH, HEIGHT) / 16;

    private final Set<Fixture> sides = new HashSet<>();

    @Getter
    private final Set<Body> edges = new HashSet<>();

    @Getter
    private Set<Edge> oldedges = new HashSet<>();

    private Set<Vec2> vertices = new HashSet<>();

    @Getter
    private Set<Score> scores = new HashSet<>();

    @Getter
    private Set<Score> oldscores = new HashSet<>();

    private volatile boolean running = true;
    private volatile boolean generateRequested = true;

    @Getter
    private final World world;

    @Getter
    private long tick = 0;

    @Getter
    private int score = 0;
    private int scorebase = 0;

    @Getter
    private int shots = 0;

    @Getter
    private int level = 0;

    private final ScheduledExecutorService exec
        = Executors.newSingleThreadScheduledExecutor();

    @Getter
    private final Body ball;
    private final Fixture ballFixture;

    private Set<Body> dead = new HashSet<>();

    public Game() {
        world = new World(new Vec2(0, 0), false);

        val top = new PolygonShape();
        top.setAsEdge(new Vec2(0, 0), new Vec2(WIDTH, 0));
        sides.add(world.createBody(new BodyDef()).createFixture(top, 0f));

        val bottom = new PolygonShape();
        bottom.setAsEdge(new Vec2(0, HEIGHT), new Vec2(WIDTH, HEIGHT));
        sides.add(world.createBody(new BodyDef()).createFixture(bottom, 0f));

        val left = new PolygonShape();
        left.setAsEdge(new Vec2(0, 0), new Vec2(0, HEIGHT));
        sides.add(world.createBody(new BodyDef()).createFixture(left, 0f));

        val right = new PolygonShape();
        right.setAsEdge(new Vec2(WIDTH, 0), new Vec2(WIDTH, HEIGHT));
        sides.add(world.createBody(new BodyDef()).createFixture(right, 0f));

        val ballshape = new CircleShape();
        ballshape.m_radius = BALL_RADIUS;
        val ballbody = new BodyDef();
        ballbody.position = randomPosition();
        ballbody.type = BodyType.DYNAMIC;
        ballbody.linearDamping = BALL_DAMPING;
        val ballfix = new FixtureDef();
        ballfix.shape = ballshape;
        ballfix.density = BALL_DENSITY;
        ballfix.friction = BALL_FRICTION;
        ballfix.restitution = BALL_RESTITUTION;
        ball = world.createBody(ballbody);
        ballFixture = ball.createFixture(ballfix);

        world.setContactListener(this);
        exec.scheduleAtFixedRate(new Runnable() {
                public void run() {
                    if (!running) {
                        return;
                    }
                    world.step(1f / FPS, V_ITERATIONS, P_ITERATIONS);
                    tick++;
                    for (Body b : dead) {
                        world.destroyBody(b);
                        edges.remove(b);
                        Edge edge = (Edge) b.getUserData();
                        oldedges.add(edge);
                        edge.setDeathTick(tick);
                        b.setUserData(tick);
                    }
                    dead = new HashSet<>();
                    if (ballStopped()) {
                        ball.setLinearVelocity(new Vec2(0, 0));
                        scorebase = 0;
                        for (Score s : scores) {
                            s.setDeathTick(tick);
                        }
                        oldscores.addAll(scores);
                        scores = new HashSet<>();
                    }
                    if (cleared() && ballStopped()) {
                        log.info("next level");
                        level++;
                        generate();
                    }
                    if (generateRequested) {
                        log.info("level generate");
                        clear();
                        generateLevel();
                    }
                    setChanged();
                    notifyObservers();
                }
            }, 0L, (long) (1000.0 / FPS), TimeUnit.MILLISECONDS);
    }

    public void start() {
        running = true;
    }

    public void stop() {
        running = false;
    }

    public void generate() {
        generateRequested = true;
    }

    private void generateLevel() {
        score += shots * 10;
        shots = 10 + level / 5;
        oldedges = new HashSet<>();
        oldscores = scores;
        scores = new HashSet<>();
        List<Vec2> roots = new ArrayList<>();
        for (int i = 0; i < level + 1; i++) {
            Vec2 p = randomPosition();
            roots.add(p);
            if (i > 0) {
                addEdge(roots.get(i - 1), roots.get(i));
            }
        }
        for (Vec2 v : roots) {
            spider(v, 0.8);
            spider(v, 0.8);
        }
        generateRequested = false;
    }

    private boolean inBounds(Vec2 p) {
        return p.x > 0 && p.x < WIDTH && p.y > 0 && p.y < HEIGHT;
    }

    private boolean nearVertex(Vec2 p) {
        for (Vec2 v : vertices) {
            if (v.sub(p).length() < MIN_EDGE) {
                return true;
            }
        }
        return false;
    }

    private void spider(Vec2 p, double prob) {
        Vec2 end = null;
        double dist, angle;
        int giveup = 0;
        do {
            if (giveup++ > 16) return;
            angle = RNG.nextFloat() * Math.PI * 2f;
            dist = RNG.nextGaussian() * prob
                * Math.min(WIDTH, HEIGHT) / 4;
            end = new Vec2((float) (Math.cos(angle) * dist),
                           (float) (Math.sin(angle) * dist)).add(p);
        } while (dist < MIN_EDGE || !inBounds(end) || nearVertex(end));
        addEdge(p, end);
        if (RNG.nextDouble() < prob) {
            spider(end, prob / 2);
        }
    }

    private void addEdge(Vec2 a, Vec2 b) {
        val shape = new PolygonShape();
        shape.setAsEdge(a, b);
        val body = world.createBody(new BodyDef());
        if (body != null) {
            edges.add(body);
            body.createFixture(shape, 0f);
            vertices.add(a);
            vertices.add(b);
            body.setUserData(new Edge(a, b));
        }
    }

    public void clear() {
        vertices = new HashSet<>();
        dead.addAll(edges);
    }

    public boolean cleared() {
        return edges.isEmpty();
    }

    private Vec2 randomPosition() {
        return new Vec2(RNG.nextFloat() * WIDTH, RNG.nextFloat() * HEIGHT);
    }

    public boolean ballStopped() {
        return ball.getLinearVelocity().length() < BALL_CUTOFF;
    }

    public void shoot(Vec2 dir) {
        shots--;
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
        Body scored = null;
        if (ballFixture != a && !sides.contains(a)) {
            scored = a.getBody();
        } else if (ballFixture != b && !sides.contains(b)) {
            scored = b.getBody();
        }
        if (scored != null) {
            dead.add(scored);
            scorebase++;
            score += scorebase;
            Vec2 p = scored.getWorldPoint(contact.getManifold().localPoint);
            scores.add(new Score(p, scorebase));
        }
    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse) {
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold) {
    }
}
