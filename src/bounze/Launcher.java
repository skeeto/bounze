package bounze;

import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JPanel;
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
        GamePanel view = new GamePanel(game);
        ScorePanel score = new ScorePanel(game);
        JFrame frame = new JFrame("Bounze");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        JPanel panel = new JPanel();
        BoxLayout layout = new BoxLayout(panel, BoxLayout.Y_AXIS);
        panel.setLayout(layout);
        panel.add(score);
        panel.add(view);
        frame.add(panel);
        frame.pack();
        frame.setVisible(true);
    }
}
