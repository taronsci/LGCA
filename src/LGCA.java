import java.util.Random;

public class LGCA {
    public static final byte EAST = 1<<0, NORTHEAST = 1<<1, NORTHWEST = 1<<2, WEST = 1<<3, SOUTHWEST = 1<<4, SOUTHEAST = 1<<5;
    public static final byte SOLID = 1<<6;

    public static final byte RAND = (byte) (1<<7);
    public static final byte PARTICLE_BITS = (byte) 0b00111111;

    int count = 0;

    // Detect 2-particle head-on collisions:
    byte oppEW = EAST | WEST;
    byte oppNESW = NORTHEAST | SOUTHWEST;
    byte oppNWSE = NORTHWEST | SOUTHEAST;

    // Detect 3-particle collisions:
    byte tri1 = EAST | NORTHWEST | SOUTHWEST;   // 0,2,4
    byte tri2 = NORTHEAST | WEST | SOUTHEAST;   // 1,3,5


    private int width, height;
    private byte[][] grid, nextGrid;
    private Random rand = new Random();

    public LGCA(int width, int height) {
        this.width = width;
        this.height = height;
        grid = new byte[height][width];

        nextGrid = new byte[height][width];
    }

    /** Initialize grid with particles or walls. */
    public void initialize() {
        // Example: clear grid and set border walls:
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
//                grid[y][x] = 0;

                if (x==0 || x==width-1 || y==0 || y==height-1) {
                    grid[y][x] |= SOLID; // mark boundary as wall
                    nextGrid[y][x] |= SOLID; // mark boundary as wall?
                }
            }
        }

        setWall();
        setup();
    }
    public void setWall() {
        for (int i = 300; i < 400; i++) { //200,300
            grid[i][300] = SOLID;
            nextGrid[i][300] = SOLID;
        }
    }

    public void setup(){
        //particles
        for(int i = 1; i < height-1; i++){ //height-1
            grid[i][1] = generateWeightedByte(new double[]{0.8,0.5,0.2,0.1,0.2,0.5});
        }
    }

    public static byte generateWeightedByte(double[] weights) {
        // weights should be of length 6 for the 6 particle bits (EAST to SOUTHEAST)
        byte result = 0;
        Random rand = new Random();

        for (int i = 0; i < 6; i++) {
            if (rand.nextDouble() < weights[i]) {
                result |= (byte) (1 << i);
            }
        }

        return result;
    }

    public void collision(){
        // Collision step: compute nextGrid (temporarily reuse nextGrid)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                
                if ((grid[y][x] & SOLID) != 0) {
                    //do nothing
                } else { // Apply FHP collision rules to non-solid cell
                    grid[y][x] = applyCollision(grid[y][x]);
                }
            }
        }
    }

    /** Apply FHP-I collision rules to a single cell's bits. */
    //after this grid is before collision state, nextGrid is after collision state
    private byte applyCollision(byte cell) {
        // Check each opposite pair:
        if ((cell == oppEW)){ //(cell & oppEW) == oppEW && (cell & ~(oppEW)) == 0) { //bits in oppEW are 1, and others are 0
            // EAST + WEST present exclusively
            cell = 0; //cell &= (byte) ~oppEW; // make 0     //grid is not changed here

            if (rand.nextBoolean()) //use RAND bit of cell
                cell |= (NORTHEAST|SOUTHWEST);
            else
                cell |= (NORTHWEST|SOUTHEAST);
            return cell;
        }
        else if (cell == oppNESW){ //(cell & oppNESW) == oppNESW && (cell & ~(oppNESW)) == 0) {
            // NORTHEAST+SOUTHWEST
            cell = 0; //&= ~oppNESW;

            if (rand.nextBoolean())
                cell |= (NORTHWEST|SOUTHEAST);
            else
                cell |= (EAST|WEST);
            return cell;
        }
        else if (cell == oppNWSE){ //(cell & oppNWSE) == oppNWSE && (cell & ~(oppNWSE)) == 0) {
            // NORTHWEST+SOUTHEAST
            cell = 0; //&= ~oppNWSE;

            if (rand.nextBoolean())
                cell |= (EAST|WEST);
            else
                cell |= (NORTHEAST|SOUTHWEST);
            return cell;
        }

        //check 3-pair collisions
        else if (cell == tri1){ //(cell & tri1) == tri1 && (cell & ~tri1) == 0) {
            // Case {0,2,4}
            cell = 0; //&= ~tri1;

            return tri2;
        }
        else if (cell == tri2){ //(cell & tri2) == tri2 && (cell & ~tri2) == 0) {
            // Case {1,3,5}
            cell = 0; //&= ~tri2;

//            cell |= tri1;

            return tri1;
        }

        return cell; // no collisions
    }

    public void propagate(){
        // Propagation step: move each bit to neighbor cell
        // We'll write into grid (swap buffers afterwards)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                byte cell = grid[y][x];

                // For each direction bit in cell, move it:
                if(y % 2 != 0) { //odd row
                    if ((cell & EAST) != 0) //means that EAST bit is 1
                        moveTo(x, y, EAST, 1, 0); //bit East of byte in location y+0 x+1 should be made 1

                    if ((cell & NORTHEAST) != 0)
                        moveTo(x, y, NORTHEAST, 1, -1);

                    if ((cell & NORTHWEST) != 0)
                        moveTo(x, y, NORTHWEST, 0, -1);

                    if ((cell & WEST) != 0)
                        moveTo(x, y, WEST, -1, 0);

                    if ((cell & SOUTHWEST) != 0)
                        moveTo(x, y, SOUTHWEST, 0, 1);

                    if ((cell & SOUTHEAST) != 0)
                        moveTo(x, y, SOUTHEAST, 1, 1);
                }
                else{ //even row
                    if ((cell & EAST) != 0)
                        moveTo(x, y, EAST, 1, 0);

                    if ((cell & NORTHEAST) != 0)
                        moveTo(x, y, NORTHEAST, 0, -1);

                    if ((cell & NORTHWEST) != 0)
                        moveTo(x, y, NORTHWEST, -1, -1);

                    if ((cell & WEST) != 0)
                        moveTo(x, y, WEST, -1, 0);

                    if ((cell & SOUTHWEST) != 0)
                        moveTo(x, y, SOUTHWEST, -1, 1);

                    if ((cell & SOUTHEAST) != 0)
                        moveTo(x, y, SOUTHEAST, 0, 1);
                }

                // Only keep the solid wall bit(s), clear the particles
                grid[y][x] &= (byte) ~PARTICLE_BITS;                        //should work but doesn't
            }
        }

        // After moving all particles, swap buffers - could also do ping-pong buffering (double buffering)
        byte[][] temp = grid;
        grid = nextGrid;
        nextGrid = temp;
    }

    /** Move a particle in direction (dx,dy) or reflect if hitting a wall. */
    private void moveTo(int x, int y, int dirBit, int dx, int dy) {
        int nx = x + dx, ny = y + dy;

//         If out of bounds or target is solid, reflect:
        if(nx >= width -1 ){ //right wall
            nextGrid[y][x] &= (byte) ~dirBit;
//            System.out.println(1);
        }
        else if (nx < 0 || ny < 0 || ny >= height || (grid[ny][nx] & SOLID) != 0) {
            // reflect: set opposite direction in original cell
//            int oppositeBit = 1 << ((Integer.numberOfTrailingZeros(dirBit) + 3) % 6);
//            nextGrid[y][x] |= (byte) oppositeBit;
            int reflected = getReflectedDirection(dirBit);
            nextGrid[y][x] |= (byte) reflected;
        } else {
            // propagate to neighbor (in next grid state)
            nextGrid[ny][nx] |= (byte) dirBit;
        }

        //instead of this, could clear all 6bits in the method above
//        grid[y][x] &= (byte) ~dirBit; //clears specified direction of byte after propagating it. grid should be empty after propagation, only SOLID, RAND bits should be alive
    }

    private int getReflectedDirection(int dirBit) {
        switch (dirBit) {
            case EAST:        return WEST;         // East reflects to West
            case NORTHEAST:   return SOUTHEAST;    // Northeast reflects to Southwest
            case NORTHWEST:   return SOUTHWEST;    // Northwest reflects to Southeast
            case WEST:        return EAST;         // West reflects to East
            case SOUTHWEST:   return NORTHWEST;    // Southwest reflects to Northeast
            case SOUTHEAST:   return NORTHEAST;    // Southeast reflects to Northwest
            default:          return 0;
        }
    }

    /** Perform one time step: collision and propagation. */
    public void step() {
        collision();
        propagate();
        count++;
        //how often particles come in
        setup();
    }

    public int getCount(){
        return count;
    }

    /** Returns the current grid (for visualization). */
    public byte[][] getGrid() {
        return grid;
    }

}

