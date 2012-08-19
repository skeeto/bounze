package bounze;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JComponent;
import lombok.NonNull;
import lombok.extern.java.Log;
import lombok.val;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;

/**
 * Display the state of a Game as a JComponent.
 */
@Log
@SuppressWarnings("serial")
public final class GamePanel extends JComponent
    implements Observer, MouseMotionListener, MouseListener, KeyListener {

    private static final Color BACK = new Color(0, 102, 153);
    private static final Color FORE = new Color(255, 255, 255);
    private static final Color FILL = new Color(0,  51, 153);
    private static final Color SCORE = Color.BLACK;

    private static final Font SCORE_FONT
        = new Font(Font.SANS_SERIF, Font.BOLD, 1);
    private static final Font GAME_OVER_FONT
        = new Font(Font.SANS_SERIF, Font.PLAIN, 3);

    /** Scale up game units by this amount. */
    public static final float SCALE = 10;
    private static final Stroke STROKE = new BasicStroke((2f / SCALE));

    private final Game game;

    @NonNull
    private Vec2 mouseLast = new Vec2(0, 0);

    private Path2D pointer = new Path2D.Double();

    /** Create a new panel displaying a game.
     * @param game  the game to display
     */
    public GamePanel(final Game game) {
        this.game = game;
        Dimension d = new Dimension((int) (Game.WIDTH * SCALE),
                                    (int) (Game.HEIGHT * SCALE));
        setPreferredSize(d);

        pointer.moveTo(3, 1);
        pointer.lineTo(3 + 1, 0);
        pointer.lineTo(3, -1);

        addMouseListener(this);
        addMouseMotionListener(this);
        addKeyListener(this);
        game.addObserver(this);
    }

    @Override
    public void paintComponent(final Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        g.setColor(BACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(FORE);
        g.setStroke(new BasicStroke(2));
        g.drawRect(1, 1, getWidth() - 2, getHeight() - 2);

        g.setStroke(STROKE);
        g.scale(SCALE, SCALE);

        g.setColor(FORE);
        for (Edge e : game.getLiveEdges()) {
            draw(g, e);
        }

        for (Edge e : game.getDeadEdges()) {
            int age = (int) (game.getTick() - e.getDeathTick());
            if (age < Game.FPS) {
                int alpha = 255 - age * 255 / Game.FPS;
                g.setColor(new Color(FORE.getRed(), FORE.getGreen(),
                                     FORE.getBlue(), alpha));
                draw(g, e);
            }
        }

        /* Draw active scores. */
        g.setFont(SCORE_FONT);
        g.setColor(SCORE);
        for (Score s : game.getLiveScores()) {
            draw(g, s);
        }

        /* Draw inactive scores. */
        for (Score s : game.getDeadScores()) {
            int age = (int) (game.getTick() - s.getDeathTick());
            if (age < Game.FPS) {
                int alpha = 255 - age * 255 / Game.FPS;
                Color c = new Color(SCORE.getRed(), SCORE.getGreen(),
                                    SCORE.getBlue(), alpha);
                g.setColor(c);
                draw(g, s);
            }
        }

        /* Draw the game ball. */
        Body ball = game.getBall();
        Fixture ballfix = ball.getFixtureList();
        draw(g, (CircleShape) ballfix.getShape(), ball.getPosition());

        /* Draw pointer. */
        if (game.ballStopped() && !game.isGameOver()) {
            AffineTransform at = new AffineTransform();
            Vec2 pos = game.getBall().getPosition();
            at.translate(pos.x, pos.y);
            at.rotate(Math.atan2(mouseLast.y - pos.y, mouseLast.x - pos.x));
            g.draw(at.createTransformedShape(pointer));
        }

        /* Draw "Game Over" text. */
        if (game.isGameOver()) {
            String msg = "Game Over";
            log.info(msg);
            g.setColor(SCORE);
            g.setFont(GAME_OVER_FONT);
            FontMetrics fm = g.getFontMetrics();
            g.drawString(msg, Game.WIDTH / 2 - fm.stringWidth(msg) / 2,
                         2 * Game.HEIGHT / 3);
        }
    }

    /**
     * Draw a score.
     * @param g  the graphics object
     * @param s  the score to draw
     */
    private void draw(final Graphics2D g, final Score s) {
        Vec2 pos = s.getPosition();
        g.drawString("+" + s.getScore(), pos.x, pos.y);
    }

    /**
     * Draw an edge.
     * @param g  the graphics object
     * @param e  the edge to draw
     */
    private void draw(final Graphics2D g, final Edge e) {
        Path2D line = new Path2D.Float();
        line.moveTo(e.getA().x, e.getA().y);
        line.lineTo(e.getB().x, e.getB().y);
        g.draw(line);
    }

    /**
     * Draw an a circle.
     * @param g    the graphics object
     * @param s    the circle to draw
     * @param pos  The circle's position
     */
    private void draw(final Graphics2D g, final CircleShape s, final Vec2 pos) {
        double x = pos.x;
        double y = pos.y;
        double r = s.m_radius;
        val circle = new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
        g.setColor(FILL);
        g.fill(circle);
        g.setColor(FORE);
        g.draw(circle);
    }

    @Override
    public void update(final Observable o, final Object arg) {
        requestFocusInWindow();
        repaint();
    }

    @Override
    public void mouseDragged(final MouseEvent e) {
        mouseLast = new Vec2(e.getX() / SCALE, e.getY() / SCALE);
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
        mouseLast = new Vec2(e.getX() / SCALE, e.getY() / SCALE);
    }

    @Override
    public void mouseClicked(final MouseEvent e) {
    }

    @Override
    public void mouseEntered(final MouseEvent e) {
    }

    @Override
    public void mouseExited(final MouseEvent e) {
    }

    @Override
    public void mousePressed(final MouseEvent e) {
        if (game.isGameOver()) {
            game.reset();
        } else if (game.ballStopped()) {
            Vec2 pos = game.getBall().getPosition();
            Vec2 dir = new Vec2(e.getX() / SCALE - pos.x,
                                e.getY() / SCALE - pos.y);
            game.shoot(dir);
        }
    }

    @Override
    public void mouseReleased(final MouseEvent e) {
    }

    @Override
    public void keyPressed(final KeyEvent e) {
    }

    @Override
    public void keyReleased(final KeyEvent e) {
    }

    @Override
    public void keyTyped(final KeyEvent e) {
         if (e.getKeyChar() == 'g') {
            game.generate();
        } else if (e.getKeyChar() == 'r') {
            game.reset();
        }
    }
}
