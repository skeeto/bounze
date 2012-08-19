package bounze;

import javax.swing.BoxLayout;
import javax.swing.JApplet;

/**
 * Run the game as an applet.
 */
@SuppressWarnings("serial")
public class BounzeApplet extends JApplet {

    /** The game object. */
    private Game game;

    @Override
    public final void init() {
        game = new Game();
        GamePanel panel = new GamePanel(game);
        ScorePanel score = new ScorePanel(game);
        BoxLayout layout = new BoxLayout(getContentPane(), BoxLayout.Y_AXIS);
        setLayout(layout);
        add(score);
        add(panel);
    }

    @Override
    public final void start() {
        game.start();
    }

    @Override
    public final void stop() {
        game.stop();
    }

}
