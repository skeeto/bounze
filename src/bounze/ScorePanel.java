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
import java.util.Observable;
import java.util.Observer;
import javax.swing.JComponent;

@SuppressWarnings("serial")
public class ScorePanel extends JComponent implements Observer {

    private static final Color BACK = Color.BLACK;
    private static final Color LABEL = new Color(255, 255, 255);
    private static final Color FORE = new Color(0,  51, 153);

    private static final Font FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 28);
    private static final Font LABEL_FONT
        = new Font(Font.SANS_SERIF, Font.BOLD, 12);
    private static final int HEIGHT = 50;
    private static final int PAD = 5;

    private static final Stroke STROKE = new BasicStroke(2);

    private final Game game;

    public ScorePanel(Game game) {
        this.game = game;
        Dimension d = new Dimension((int) (Game.WIDTH * GamePanel.SCALE),
                                    HEIGHT);
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

        String score = "" + game.getScore();
        String shots = "" + game.getShots();
        String level = "" + game.getLevel();

        g.setFont(FONT);
        FontMetrics fm = g.getFontMetrics();
        g.setColor(FORE);
        g.drawString(score, PAD, HEIGHT - PAD);
        g.drawString(level, getWidth() / 2 - fm.stringWidth(level) / 2,
                     HEIGHT - PAD);
        g.drawString(shots, getWidth() - PAD - fm.stringWidth(shots),
                     HEIGHT - PAD);

        String sscore = "score";
        String slevel = "level";
        String sshots = "shots";

        g.setColor(LABEL);
        g.setFont(LABEL_FONT);
        fm = g.getFontMetrics();
        g.drawString(sscore, PAD, fm.getAscent());
        g.drawString(slevel, getWidth() / 2 - fm.stringWidth(slevel) / 2,
                     fm.getAscent());
        g.drawString(sshots, getWidth() - PAD - fm.stringWidth(sshots),
                     fm.getAscent());

        g.setStroke(STROKE);
        g.drawLine(PAD, fm.getAscent() + PAD,
                   2 * PAD + fm.stringWidth(slevel), fm.getAscent() + PAD);
        g.drawLine(getWidth() / 2 - fm.stringWidth(slevel) / 2 - PAD,
                   fm.getAscent() + PAD,
                   getWidth() / 2 + fm.stringWidth(slevel) / 2 + PAD,
                   fm.getAscent() + PAD);
        g.drawLine(getWidth() - 2 * PAD - fm.stringWidth(sshots),
                   fm.getAscent() + PAD,
                   getWidth() - PAD, fm.getAscent() + PAD);
    }

    @Override
    public void update(Observable o, Object arg) {
        repaint();
    }
}
