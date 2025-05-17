import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

public class VisualizerAVG extends JPanel {
    // Visualization modes
    public static final int MODE_CELL = 0;
    public static final int MODE_BLOCK = 1;
    public static final int MODE_EDDY = 2;

    private int visMode = MODE_CELL;
    private int cellSize;       // pixel size of one lattice cell
    private int blockSize;       // block size for averaging
//    private boolean[][][] lattice;   // [x][y][direction] occupancy (6 directions)
    private LGCA model;

    // Define the 6 unit velocity vectors for D2Q6 (y-axis downwards).
    private final double sqrt3 = Math.sqrt(3.0);

    private final double[][] directions = {
            {1.0, 0.0},  // East
            {0.5, -sqrt3 / 2},  // North-East
            {-0.5, -sqrt3 / 2},  // North-West
            {-1.0, 0.0},  // West
            {-0.5, sqrt3 / 2},  // South-West
            {0.5, sqrt3 / 2}   // South-East
    };

    public VisualizerAVG(LGCA model, int cellSize, int blockSize, int mode) {
        this.model = model;
        this.cellSize = cellSize;
        this.blockSize = blockSize;
        setMode(mode);
        setPreferredSize(new Dimension(model.getGrid()[0].length * cellSize,model.getGrid().length * cellSize));
    }

    public void setMode(int mode) {
        this.visMode = mode;
    }

//    public void setLattice(boolean[][][] lattice) {
//        this.lattice = lattice;
//        repaint();
//    }

    public void setCellSize(int sz) {
        this.cellSize = sz;
        repaint();
    }

    public void setBlockSize(int bs) {
        this.blockSize = bs;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (model.getGrid() == null)
            return;
        byte[][] grid = model.getGrid();
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(Color.BLUE);  // color for arrows (can change per mode)
        int nx = grid[0].length;
        int ny = grid.length;

        // 1) Cell-based arrows
        if (visMode == MODE_CELL) {
            // For each cell and each direction, draw arrow if particle present.
            for (int x = 0; x < nx; x++) {
                for (int y = 0; y < ny; y++) {
                    // Center of this cell in pixels
                    double cx = (x + 0.5) * cellSize;
                    double cy = (y + 0.5) * cellSize;

                    if ((grid[y][x] & LGCA.SOLID) != 0) {
                        g.setColor(Color.BLACK);
                        g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                        continue;
                    }
                    g.setColor(Color.BLUE);

                    // Draw arrows for each occupied direction
                    for (int d = 0; d < 6; d++) {
                        if ((grid[y][x] & 1 << d) != 0) { //if bit d is 1
                            // Scale arrow length so it fits in cell
                            double scale = cellSize * 0.4;
                            double vx = directions[d][0] * scale;
                            double vy = directions[d][1] * scale;

                            drawArrow(g2, cx, cy, vx, vy);
                        }
                    }
                }
            }
        }
        // 2) Block-averaged or Eddy visualization
        else {
            int blocksX = nx / blockSize;
            int blocksY = ny / blockSize;
            // Sum velocity vectors per block
            double[][] sumX = new double[blocksX][blocksY];
            double[][] sumY = new double[blocksX][blocksY];

            for (int bx = 0; bx < blocksX; bx++) {
                for (int by = 0; by < blocksY; by++) {
                    double sx = 0, sy = 0;
                    for (int i = 0; i < blockSize; i++) {
                        for (int j = 0; j < blockSize; j++) {
                            int x = bx * blockSize + i;
                            int y = by * blockSize + j;

                            if (x >= nx || y >= ny)
                                continue;

                            if ((grid[y][x] & LGCA.SOLID) != 0) {
                                g.setColor(Color.BLACK);
                                g.fillRect(x * cellSize, y * cellSize, cellSize, cellSize);
                                continue;
                            }
                            g.setColor(Color.BLUE);

                            for (int d = 0; d < 6; d++) {
                                if ((grid[y][x] & 1<<d) != 0) {
                                    sx += directions[d][0]; //vel_sum_x
                                    sy += directions[d][1]; //vel_sum_y
                                }
                            }
                        }
                    }
                    sumX[bx][by] = sx; //momentum[0]
                    sumY[bx][by] = sy; //momentum[1]
                }
            }
            // Compute global average (for Eddy mode)
            double globalX = 0, globalY = 0;
            if (visMode == MODE_EDDY) {
                for (int bx = 0; bx < blocksX; bx++) {
                    for (int by = 0; by < blocksY; by++) {
                        globalX += sumX[bx][by];
                        globalY += sumY[bx][by];
                    }
                }
                int numBlocks = blocksX * blocksY;
                if (numBlocks > 0) {
                    globalX /= numBlocks;
                    globalY /= numBlocks;
                }
            }
            // Draw one arrow per block
            for (int bx = 0; bx < blocksX; bx++) {
                for (int by = 0; by < blocksY; by++) {
                    double vx = sumX[bx][by];
                    double vy = sumY[bx][by];

                    // Subtract global mean if in Eddy mode
                    if (visMode == MODE_EDDY) {
                        vx -= globalX;
                        vy -= globalY;
                    }

                    // Compute drawing scale so arrows fit in block region
//                    double scale = (double) cellSize * 0.5/ blockSize;
                    double scale = (double) cellSize *10/(blockSize * blockSize);
                    double dx = vx * scale;
                    double dy = vy * scale;

                    // Position arrow at center of block (in pixels)
                    double cx = (bx * blockSize + blockSize / 2.0) * cellSize;
                    double cy = (by * blockSize + blockSize / 2.0) * cellSize;
                    drawArrow(g2, cx, cy, dx, dy);
                }
            }
        }
    }

    /**
     * Draws an arrow (line + arrowhead) from (x,y) in direction (vx,vy).
     */
    private void drawArrow(Graphics2D g2, double x, double y, double vx, double vy) {
        double len = Math.hypot(vx, vy);
        if (len < 1e-6)
            return;  // no arrow if vector is zero

        double ux = vx / len;
        double uy = vy / len;

        // End point of arrow line
        double tx = x + vx;
        double ty = y + vy;

        // Draw main line
        g2.draw(new Line2D.Double(x, y, tx, ty));

        // Arrowhead size
        double headLen = 3.0; //6
        double headWidth = 2.0; //4

        // Base of arrowhead (point where it starts)
        double bx = tx - ux * headLen;
        double by = ty - uy * headLen;

        // Perpendicular vector to (ux,uy)
        double perpX = -uy;
        double perpY = ux;

        // Two corners of the arrowhead triangle
        double cx1 = bx + perpX * (headWidth / 2);
        double cy1 = by + perpY * (headWidth / 2);
        double cx2 = bx - perpX * (headWidth / 2);
        double cy2 = by - perpY * (headWidth / 2);

        // Create triangle and fill
        Polygon arrowHead = new Polygon();
        arrowHead.addPoint((int) tx, (int) ty);
        arrowHead.addPoint((int) cx1, (int) cy1);
        arrowHead.addPoint((int) cx2, (int) cy2);
        g2.fill(arrowHead);
    }

    public void start() {
        Timer timer = new Timer(100, e -> {
            model.step();
            repaint();
        });

        timer.start();
    }

}


