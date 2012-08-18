package bounze;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.Stroke;
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
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;

@SuppressWarnings("serial")
public class GamePanel extends JComponent
    implements Observer, MouseMotionListener, MouseListener {

    private static final Color BACK = new Color(  0, 102, 153);
    private static final Color FORE = new Color(255, 255, 255);
    private static final Color FILL = new Color(  0,  51, 153);

    private static final int SCALE = 10;
    private static final Stroke STROKE = new BasicStroke((2f / SCALE));

    private final Game game;

    @NonNull
    private Vec2 mouseLast = new Vec2(0, 0);

    private Path2D pointer = new Path2D.Double();

    public GamePanel(Game game) {
        this.game = game;
        Dimension d = new Dimension(Game.WIDTH * SCALE + 1,
                                    Game.HEIGHT * SCALE + 1);
        setPreferredSize(d);

        pointer.moveTo(3, 1);
        pointer.lineTo(3 + 1, 0);
        pointer.lineTo(3, -1);

        addMouseListener(this);
        addMouseMotionListener(this);
        game.addObserver(this);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        g.setColor(BACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setStroke(STROKE);
        g.scale(SCALE, SCALE);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        Body body = game.getWorld().getBodyList();
        while (body != null) {
            Vec2 pos = body.getPosition();
            float angle = body.getAngle();
            Fixture fixture = body.getFixtureList();
            while (fixture != null) {
                Shape shape = fixture.getShape();
                if (shape instanceof CircleShape) {
                    double x = pos.x;
                    double y = pos.y;
                    double r = shape.m_radius;
                    val circle
                        = new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
                    g.setColor(FILL);
                    g.fill(circle);
                    g.setColor(FORE);
                    g.draw(circle);
                } else if (shape instanceof PolygonShape) {
                    PolygonShape s = (PolygonShape) shape;
                    Path2D path = new Path2D.Float();
                    Vec2 first = s.getVertex(0);
                    path.moveTo(first.x, first.y);
                    for (int i = 1; i < s.getVertexCount(); i++) {
                        Vec2 v = s.getVertex(i);
                        path.lineTo(v.x, v.y);
                    }
                    //path.closePath();
                    AffineTransform at = new AffineTransform();
                    at.translate(pos.x, pos.y);
                    at.rotate(angle);
                    g.setColor(FORE);
                    g.draw(at.createTransformedShape(path));
                } else {
                    System.out.println("Cannot draw shape: " + shape);
                }
                fixture = fixture.getNext();
            }
            body = body.getNext();
        }

        if (game.ballStopped()) {
            AffineTransform at = new AffineTransform();
            Vec2 pos = game.getBall().getPosition();
            at.translate(pos.x, pos.y);
            at.rotate(Math.atan2(mouseLast.y - pos.y, mouseLast.x - pos.x));
            g.draw(at.createTransformedShape(pointer));
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseLast = new Vec2(e.getX() / SCALE, e.getY() / SCALE);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseLast = new Vec2(e.getX() / SCALE, e.getY() / SCALE);
    }

    @Override
    public void	mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (game.ballStopped()) {
            Vec2 pos = game.getBall().getPosition();
            Vec2 dir = new Vec2(e.getX() / SCALE - pos.x,
                                e.getY() / SCALE - pos.y);
            game.shoot(dir);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }
}
