import javax.swing.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;

public class Main {
    public static void main(String[] args) {
        int width = 1000, height = 600; //1000, 500
        LGCA model = new LGCA(width, height);
        model.initialize();

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FHP-I Lattice Gas");
//            Visualizer viz = new Visualizer(model);
            VisualizerAVG viz = new VisualizerAVG(model,2,16,0);

            frame.add(viz);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setVisible(true);

            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    // Place your statistics output here
                    System.out.println("Simulation ended.");
//                    System.out.println("Total particles: " + simulation.getParticleCount());
                    System.out.println("Total steps: " + model.getCount());
                }
            });

            viz.start();  // begin animation
        });
    }
}