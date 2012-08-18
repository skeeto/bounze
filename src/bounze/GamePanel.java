package bounze;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JComponent;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;

@SuppressWarnings("serial")
public class GamePanel extends JComponent implements Observer {

    private static final Color BACK = new Color(0f, 0.1f, 0.5f);
    private static final Color FORE = new Color(1f, 1f, 1f);

    private static final int SCALE = 10;

    private final Game game;

    public GamePanel(Game game) {
        this.game = game;
        Dimension d = new Dimension(Game.WIDTH * SCALE, Game.HEIGHT * SCALE);
        setPreferredSize(d);
        game.addObserver(this);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        g.setColor(BACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                           RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(FORE);
        Body body = game.getWorld().getBodyList();
        while (body != null) {
            Vec2 pos = body.getPosition();
            float angle = body.getAngle();
            Fixture fixture = body.getFixtureList();
            while (fixture != null) {
                Shape shape = fixture.getShape();
                if (shape instanceof CircleShape) {
                    //draw(g, pos, (CircleShape) shape);
                    double x = pos.x * SCALE;
                    double y = pos.y * SCALE;
                    double r = shape.m_radius * SCALE;
                    val circle
                        = new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
                    g.draw(circle);
                } else if (shape instanceof PolygonShape) {
                    //draw(g, pos, angle, (PolygonShape) shape);
                } else {
                    System.out.println("Cannot draw shape: " + shape);
                }
                fixture = fixture.getNext();
            }

            body = body.getNext();
        }
    }

    @Override
    public void update(Observable o, Object arg) {
        repaint();
    }
}
