package bounze;

import javax.swing.JApplet;

@SuppressWarnings("serial")
public class BounzeApplet extends JApplet {

    private Game game;

    @Override
    public final void init() {
        game = new Game();
        GamePanel panel = new GamePanel(game);
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
