package bounze;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Random;
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
import org.pcollections.HashTreePSet;
import org.pcollections.PSet;

/**
 * An instance of a game of Bounze. Can be restarted when the game ends.
 */
@Log
public final class Game extends Observable implements ContactListener {

    /** Frames per second.  */
    public static final int FPS = 30;

    /* JBox2D parameters. */
    private static final int V_ITERATIONS = 8;
    private static final int P_ITERATIONS = 3;

    /** World width. */
    public static final int WIDTH = 56;

    /** World height. */
    public static final int HEIGHT = 36;

    /* JBox2D ball parameters. */
    private static final float BALL_RADIUS = 1.25f;
    private static final float BALL_DENSITY = 1f;
    private static final float BALL_FRICTION = 0f;
    private static final float BALL_RESTITUTION = 0.85f;
    private static final float BALL_DAMPING = 0.7f;
    private static final float BALL_CUTOFF = 5.0f;
    private static final float BALL_VELOCITY = 60.0f;

    /** Priavte random number generator. */
    private static final Random RNG = new Random();

    /** Minimum edge length. */
    private static final float MIN_EDGE = Math.max(WIDTH, HEIGHT) / 16;

    /** Active edges in the world. */
    @Getter
    private PSet<Edge> liveEdges = HashTreePSet.empty();

    /** Inactive edges of the world. */
    @Getter
    private PSet<Edge> deadEdges = HashTreePSet.empty();

    /** Vertices of the current level. */
    private PSet<Vec2> vertices = HashTreePSet.empty();

    /** Active fading floating scores. */
    @Getter
    private PSet<Score> liveScores = HashTreePSet.empty();

    /** Inactive fading floating scores. */
    @Getter
    private PSet<Score> deadScores = HashTreePSet.empty();

    /** True if the game has ended. */
    @Getter
    private boolean gameOver = false;

    /** True if the game is currently running. */
    private volatile boolean running = true;

    /** True if a level generation is requested. */
    private volatile boolean generateRequested = true;

    /** The JBox2D world. */
    @Getter
    private final World world;

    /** The step counter. */
    @Getter
    private long tick = 0;

    /** Current player score. */
    @Getter
    private int score = 0;

    /** Current base score counter: score increases by this for each hit. */
    private int scorebase = 0;

    /** Number of shots left. */
    @Getter
    private int shots = 0;

    /** Current level number. */
    @Getter
    private int level = 0;

    /** Thread that drives the simulation forward. */
    private final ScheduledExecutorService exec
        = Executors.newSingleThreadScheduledExecutor();

    /** The game ball -- interactive with by the player. */
    @Getter
    private final Body ball;

    /** The ball's JBox2D fixture. */
    private final Fixture ballFixture;

    /** Bodies to be removed before the next simulation step. */
    private PSet<Body> dead = HashTreePSet.empty();

    /**
     * Create a new game instance.
     */
    public Game() {
        world = new World(new Vec2(0, 0), false);

        /* Create world edges. */
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

        /* Set up the ball. */
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

        /* Set up the simulation thread. */
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
                        Edge edge = (Edge) b.getUserData();
                        liveEdges = liveEdges.minus(edge);
                        deadEdges = deadEdges.plus(edge);
                        edge.setDeathTick(tick);
                    }
                    dead = HashTreePSet.empty();
                    if (ballStopped()) {
                        ball.setLinearVelocity(new Vec2(0, 0));
                        scorebase = 0;
                        for (Score s : liveScores) {
                            s.setDeathTick(tick);
                        }
                        deadScores = deadScores.plusAll(liveScores);
                        liveScores = HashTreePSet.empty();
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
                    if (ballStopped() && shots == 0 && level > 0) {
                        gameOver = true;
                    }
                    setChanged();
                    notifyObservers();
                }
            }, 0L, (long) (1000.0 / FPS), TimeUnit.MILLISECONDS);
    }

    /** Run the simulation. */
    public void start() {
        running = true;
    }

    /** Pause the simulation. */
    public void stop() {
        running = false;
    }

    /** Generate a new level (asynchronously). */
    public void generate() {
        generateRequested = true;
    }

    /** Generate a new level -- must be run by the simulation thread. */
    private void generateLevel() {
        score += shots * 10;
        shots = 10 + level / 5;
        deadEdges = HashTreePSet.empty();
        deadScores = liveScores;
        liveScores = HashTreePSet.empty();
        List<Vec2> roots = new ArrayList<Vec2>();
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

    /** Return true of the point is inside the world.
     * @param p  the point to test
     * @return true if the point is inside the world
     */
    private boolean inBounds(final Vec2 p) {
        return p.x > 0 && p.x < WIDTH && p.y > 0 && p.y < HEIGHT;
    }

    /**
     * Check to see if this point is close to an existing point.
     * @param p  the point to test
     * @return true if the point is near an existing point
     */
    private boolean nearVertex(final Vec2 p) {
        for (Vec2 v : vertices) {
            if (v.sub(p).length() < MIN_EDGE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Create a sprawling chain of edges.
     * @param p     the root point
     * @param prob  the probability of continuing
     */
    private void spider(final Vec2 p, final double prob) {
        Vec2 end = null;
        double dist, angle;
        int giveup = 0;
        do {
            if (giveup++ > 16) {
                return;
            }
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

    /**
     * Add an edge to the world.
     * @param a  the start point
     * @param b  the end point
     */
    private void addEdge(final Vec2 a, final Vec2 b) {
        val shape = new PolygonShape();
        shape.setAsEdge(a, b);
        val body = world.createBody(new BodyDef());
        if (body != null) {
            body.createFixture(shape, 0f);
            vertices = vertices.plus(a);
            vertices = vertices.plus(b);
            Edge edge = new Edge(a, b, body);
            body.setUserData(edge);
            liveEdges = liveEdges.plus(edge);
        }
    }

    /**
     * Clear all edges from the map.
     */
    public void clear() {
        vertices = HashTreePSet.empty();
        for (Edge e : liveEdges) {
            dead = dead.plus(e.getBody());
        }
    }


    /**
     * Reset the game (i.e. after a game over).
     */
    public void reset() {
        gameOver = false;
        score = 0;
        shots = 0;
        scorebase = 0;
        clear();
        ball.setTransform(new Vec2(WIDTH / 2, HEIGHT / 2), 0f);
        ball.setLinearVelocity(new Vec2(0, 0));
        level = 0;
        generate();
    }

    /**
     * Return true if all the edges have been cleared.
     * @return true if no edges remain
     */
    public boolean cleared() {
        return liveEdges.isEmpty();
    }

    /**
     * Return a uniformly distributed random position in the world.
     * @return a random world position
     */
    private Vec2 randomPosition() {
        return new Vec2(RNG.nextFloat() * WIDTH, RNG.nextFloat() * HEIGHT);
    }

    /**
     * Return true if the ball is not moving.
     * @return true if the ball is not moving
     */
    public boolean ballStopped() {
        return ball.getLinearVelocity().length() < BALL_CUTOFF;
    }

    /**
     * Shoot the ball in a direction.
     * @param dir  the direction to shoot the ball
     */
    public void shoot(final Vec2 dir) {
        shots--;
        dir.normalize();
        ball.setLinearVelocity(dir.mul(BALL_VELOCITY));
    }

    @Override
    public void beginContact(final Contact contact) {
    }

    @Override
    public void endContact(final Contact contact) {
        Body a = contact.getFixtureA().getBody();
        Body b = contact.getFixtureB().getBody();
        Body scored = null;
        Object ad = a.getUserData();
        Object bd = b.getUserData();
        if (ad != null && liveEdges.contains((Edge) ad)) {
            scored = a;
        } else if (bd != null && liveEdges.contains((Edge) bd)) {
            scored = b;
        }
        if (scored != null) {
            dead = dead.plus(scored);
            scorebase++;
            score += scorebase;
            Vec2 p = scored.getWorldPoint(contact.getManifold().localPoint);
            liveScores = liveScores.plus(new Score(p, scorebase));
        }
    }

    @Override
    public void postSolve(final Contact contact, final ContactImpulse impulse) {
    }

    @Override
    public void preSolve(final Contact contact, final Manifold oldManifold) {
    }
}
