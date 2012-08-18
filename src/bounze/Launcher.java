package bounze;

import javax.swing.JFrame;
import lombok.extern.java.Log;

@Log
public class Launcher {

    public static void main(String[] args) {
        try {
            /* Fix for poor OpenJDK performance. */
            System.setProperty("sun.java2d.pmoffscreen", "false");
        } catch (java.security.AccessControlException e) {
            log.info("could not set sun.java2d.pmoffscreen");
        }

        Game game = new Game();
        GamePanel panel = new GamePanel(game);
        JFrame frame = new JFrame("Bounze");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }
}
