package bounze;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import javax.swing.JComponent;

@SuppressWarnings("serial")
public class ScorePanel extends JComponent {

    private static final Color BACK = Color.BLACK;

    private static final int HEIGHT = 50;

    private final Game game;


    public ScorePanel(Game game) {
        this.game = game;
        Dimension d = new Dimension((int) (Game.WIDTH * GamePanel.SCALE),
                                    HEIGHT);
        setPreferredSize(d);
    }

    @Override
    public void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        g.setColor(BACK);
        g.fillRect(0, 0, getWidth(), getHeight());
    }
}
