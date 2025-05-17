import javax.swing.*;
import java.awt.*;

public class Visualizer extends JPanel {
    private LGCA model;
    private int cellSize = 1; // pixels per cell

    public Visualizer(LGCA model) {
        this.model = model;

        // Set preferred size based on model dimensions
        setPreferredSize(new Dimension(model.getGrid()[0].length * cellSize,model.getGrid().length * cellSize));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        byte[][] grid = model.getGrid();

        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                byte cell = grid[y][x];

                if ((cell & LGCA.SOLID) != 0) {
                    g.setColor(Color.BLACK);
                    g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                }
                else {
                    // Color based on number of particles:

                    int count = Integer.bitCount(cell & LGCA.PARTICLE_BITS);

                    // More particles -> brighter color:
//                    int intensity = Math.min(255, 50 + 40 * count);
                    int intensity = Math.max(0, 255 - 40 * count);

                    g.setColor(new Color(intensity, intensity, 255)); //new Color(intensity, intensity, 255)
                    g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                }
                // else leave background (empty)
            }
        }
    }

    /** Start an animation timer to step the model and repaint. */
    public void start() {
        Timer timer = new Timer(100, e -> {
            model.step();
            repaint();
        });

        timer.start();
    }
}

