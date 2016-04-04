import java.util.ArrayList;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// Represents a single square of the game area
class Cell {
    // represents the size of a cell for drawing
    static final int CELLSIZE = 10;
    
    // represents absolute height of this cell, in feet
    double height;
    // In logical coordinates, with the origin at the top-left corner of the
    // screen
    int x;
    int y;
    // the four adjacent cells to this one
    Cell left;
    Cell top;
    Cell right;
    Cell bottom;

    // reports whether this cell is flooded or not
    boolean isFlooded;

    // Set the neighbors of this cell
    // EFFECT: Modifies the left, top, right, and bottom fields of the Cell
    void setNeighbors(Cell left, Cell top, Cell right, Cell bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    Cell(double height, int x, int y) {
        this.height = height;
        this.x = x;
        this.y = y;
    };

    // mix two colors based on factor
    public Color mix(Color a, Color b, double mix) {
        if (mix > 1.0d || mix < 0.0d) {
            throw new IllegalArgumentException(
                    "Mix not between 0.0 and 1.0: " + mix);
        }
        float red = (float) ((a.getRed() * mix) / 255
                + (b.getRed() * (1 - mix)) / 255);
        float green = (float) ((a.getGreen() * mix) / 255
                + (b.getGreen() * (1 - mix)) / 255);
        float blue = (float) ((a.getBlue() * mix) / 255
                + (b.getBlue() * (1 - mix)) / 255);

        return new Color(red, green, blue);
    }

    // draw this cell based on the water height and the maximum height of the
    // island
    public WorldImage draw(int waterHeight, int maxHeight) {
        Color maxNoFlood = Color.white;
        Color minNoFlood = new Color(0.0f, 0.5f, 0.0f);
        Color minToFlood = new Color(0.25f, 0.5f, 0.0f);
        Color maxToFlood = Color.red;
        Color minFlooded = new Color(0.0f, 0.35f, 0.5f);
        Color maxFlooded = new Color(0.0f, 0.0f, 1.0f);

        if (this.isFlooded) {
            return new RectangleImage(CELLSIZE, CELLSIZE, OutlineMode.SOLID,
                    this.mix(maxFlooded, minFlooded,
                            Math.min(Math.sqrt(
                                    (waterHeight - this.height) / maxHeight),
                            1.0f)));
        }

        if (this.height - waterHeight > 0) {
            return new RectangleImage(CELLSIZE, CELLSIZE, OutlineMode.SOLID,
                    this.mix(maxNoFlood, minNoFlood,
                            (this.height - waterHeight) / maxHeight));
        }
        else {
            return new RectangleImage(CELLSIZE, CELLSIZE, OutlineMode.SOLID,
                    this.mix(maxToFlood, minToFlood,
                            Math.min(Math.sqrt(
                                    (waterHeight - this.height) / maxHeight),
                            1.0f)));
        }
    }

    // flood this cell
    // EFFECT: sets the isFlooded flag
    void flood(int waterHeight) {
        if (this.height < waterHeight && !this.isFlooded) {
            this.isFlooded = true;
            this.left.flood(waterHeight);
            this.top.flood(waterHeight);
            this.right.flood(waterHeight);
            this.bottom.flood(waterHeight);
        }
    }

    // add self to list if you are an ocean cell
    // EFFECT: modifies the list
    void addIfOcean(ArrayList<OceanCell> list) {
        // do nothing
    }
}

// An OceanCell
class OceanCell extends Cell {
    OceanCell(double height, int x, int y) {
        super(height, x, y);
        this.isFlooded = true;
    }

    OceanCell(int x, int y) {
        super(0, x, y);
        this.isFlooded = true;
    }

    // draw this OceanCell based on water height and max height
    public WorldImage draw(int waterHeight, int maxHeight) {
        return new RectangleImage(CELLSIZE, CELLSIZE, OutlineMode.SOLID, Color.BLUE);
    }

    // flood this oceanCell
    void flood(int waterHeight) {
        // do nothing
    }

    // add self to list if you are an ocean cell
    // EFFECT: modifies the list
    void addIfOcean(ArrayList<OceanCell> list) {
        list.add(this);
    }
}

// Represents an IslandGenerator generally
abstract class AIslandGenerator {
    // Defines an int constant
    static final int ISLAND_SIZE = 64;

    // Maximum height of this island
    int maxHeight;

    // calculate ManhattanDistance
    double manhattanDistance(int x, int y, int centerX, int centerY) {
        return Math.abs(x - centerX) + Math.abs(y - centerY);
    }

    // generate the heights of the cells on the island
    abstract ArrayList<ArrayList<Double>> generateHeights();

    // generate the cells of the island based on the heights
    abstract ArrayList<ArrayList<Cell>> generateCells(
            ArrayList<ArrayList<Double>> heights);

    // Fix the neighbors of the cells
    ArrayList<ArrayList<Cell>> fixNeighbors(ArrayList<ArrayList<Cell>> cells) {
        ArrayList<ArrayList<Cell>> result = new ArrayList<ArrayList<Cell>>();
        for (ArrayList<Cell> curRow : cells) {
            ArrayList<Cell> resultCurRow = new ArrayList<Cell>();
            for (Cell cur : curRow) {
                Cell left;
                Cell top;
                Cell right;
                Cell bottom;

                left = cells.get(cur.y).get(Math.max(cur.x - 1, 0));
                top = cells.get(Math.max(cur.y - 1, 0)).get(cur.x);
                right = cells.get(cur.y).get(
                        Math.min(cur.x + 1, AIslandGenerator.ISLAND_SIZE));
                bottom = cells.get(
                        Math.min(cur.y + 1, AIslandGenerator.ISLAND_SIZE))
                        .get(cur.x);

                cur.setNeighbors(left, top, right, bottom);
                resultCurRow.add(cur);
            }
            result.add(resultCurRow);
        }
        return result;
    }

    // generate the terrain
    public IList<Cell> generateTerrain() {
        ArrayList<ArrayList<Double>> heights = this.generateHeights();
        ArrayList<ArrayList<Cell>> cells = this.generateCells(heights);
        ArrayList<ArrayList<Cell>> fixedCells = this.fixNeighbors(cells);
        ArrayList<Cell> reversed = new ArrayList<Cell>();

        for (ArrayList<Cell> row : fixedCells) {
            for (Cell cell : row) {
                reversed.add(0, cell);
            }
        }

        IList<Cell> result = new Empty<Cell>();
        for (Cell cell : reversed) {
            result = new Cons<Cell>(cell, result);
        }

        return result;
    }

    AIslandGenerator() {
        this.maxHeight = AIslandGenerator.ISLAND_SIZE / 2;
    }

    AIslandGenerator(int maxHeight) {
        this.maxHeight = maxHeight;
    }
}

// A Diamond-shaped AIslandGenerator
abstract class DiamondIslandGenerator extends AIslandGenerator {
    // The distance from the center of the island at which the ocean starts
    // (32 by default)
    int oceanDistance;

    // generate the cells for this mountain island based on their heights
    public ArrayList<ArrayList<Cell>> generateCells(
            ArrayList<ArrayList<Double>> heights) {
        int centerX = AIslandGenerator.ISLAND_SIZE / 2;
        int centerY = AIslandGenerator.ISLAND_SIZE / 2;

        ArrayList<ArrayList<Cell>> result = new ArrayList<ArrayList<Cell>>();

        for (int i = 0; i < heights.size(); i += 1) {
            ArrayList<Cell> cellRow = new ArrayList<Cell>();
            for (int j = 0; j < heights.get(i).size(); j += 1) {
                if (this.manhattanDistance(j, i, centerX,
                        centerY) < this.oceanDistance) {
                    double height = heights.get(i).get(j);
                    cellRow.add(new Cell(height, j, i));
                }
                else {
                    cellRow.add(new OceanCell(j, i));
                }
            }
            result.add(cellRow);
        }
        return result;
    }

    DiamondIslandGenerator() {
        this.oceanDistance = 32;
    }

    DiamondIslandGenerator(int oceanDistance) {
        this.oceanDistance = oceanDistance;
    }
}

// A Mountain AIslandGenerator
class MountainIslandGenerator extends DiamondIslandGenerator {
    // generate the heights of the cells on this mountain island
    public ArrayList<ArrayList<Double>> generateHeights() {
        int centerX = AIslandGenerator.ISLAND_SIZE / 2;
        int centerY = AIslandGenerator.ISLAND_SIZE / 2;

        // initialize the heights of the cells in this island
        ArrayList<ArrayList<Double>> heights = new ArrayList<ArrayList<Double>>();
        // iterate over the rows (Y coordinates)
        for (int i = 0; i <= AIslandGenerator.ISLAND_SIZE; i += 1) {
            // Create a temporary ArrayList<Double> for this row
            ArrayList<Double> curRow = new ArrayList<Double>();

            // iterate over the columns (X coordinates)
            for (int j = 0; j <= AIslandGenerator.ISLAND_SIZE; j += 1) {
                // create cells with their heights based on Manhattan distance
                curRow.add(this.maxHeight
                        - this.manhattanDistance(j, i, centerX, centerY));
            }

            // add the current row to the list of heights
            heights.add(curRow);
        }

        return heights;
    }

}

// A Diamond-shaped island with random heights
class RandomIslandGenerator extends DiamondIslandGenerator {
    // generate the heights of the cells on this random island
    public ArrayList<ArrayList<Double>> generateHeights() {

        Random r = new Random();

        // initialize the heights of the cells in this island
        ArrayList<ArrayList<Double>> heights = new ArrayList<ArrayList<Double>>();
        // iterate over the rows (Y coordinates)
        for (int i = 0; i <= AIslandGenerator.ISLAND_SIZE; i += 1) {
            // Create a temporary ArrayList<Double> for this row
            ArrayList<Double> curRow = new ArrayList<Double>();

            // iterate over the columns (X coordinates)
            for (int j = 0; j <= AIslandGenerator.ISLAND_SIZE; j += 1) {
                // create cells with their heights determined randomly from 0 to
                // maxSize
                curRow.add(r.nextInt(this.maxHeight + 1) * 1.0);
            }

            // add the current row to the list of heights
            heights.add(curRow);
        }

        return heights;
    }

    RandomIslandGenerator() {
        this.maxHeight = 64;
    }
}

class RandomTerrainIslandGenerator extends AIslandGenerator {
    // generate the nudge
    double nudge(double area) {
        if (Math.random() <= .5) {
            return -1 * Math.random() * area
                    + (Math.random() * this.maxHeight) / this.maxHeight;
        }
        else {
            return Math.random() * area
                    + (Math.random() * this.maxHeight) / this.maxHeight;
        }
    }

    // generate the heights of the cells on this random terrain island
    public ArrayList<ArrayList<Double>> generateHeights() {
        // Initialize the arraylist to be IslandSize + 1 columns and rows big
        ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>(
                AIslandGenerator.ISLAND_SIZE + 1);
        for (int i = 0; i < AIslandGenerator.ISLAND_SIZE + 1; i += 1) {
            ArrayList<Double> row = new ArrayList<Double>(
                    AIslandGenerator.ISLAND_SIZE + 1);
            for (int j = 0; j < AIslandGenerator.ISLAND_SIZE + 1; j += 1) {
                row.add(0d);
            }
            result.add(row);
        }

        // set the center of the arraylist to the max height
        int centerX = (AIslandGenerator.ISLAND_SIZE + 1) / 2;
        int centerY = (AIslandGenerator.ISLAND_SIZE + 1) / 2;
        result.get(centerY).set(centerX, (double) this.maxHeight);

        // set the edges to height 1
        result.get(0).set(centerX, 1d);
        result.get(AIslandGenerator.ISLAND_SIZE - 1).set(centerX, 1d);
        result.get(centerY).set(0, 1d);
        result.get(centerY).set(AIslandGenerator.ISLAND_SIZE - 1, 1d);

        this.generateTerrain(result, 0, 0, centerX, 0, centerX, centerY, 0,
                centerY);
        this.generateTerrain(result, centerX, 0, result.size() - 1, 0,
                result.size() - 1, centerY, centerX, centerY);
        this.generateTerrain(result, centerX, centerY, result.size() - 1,
                centerY, result.size() - 1, result.size() - 1, centerX,
                result.size() - 1);
        this.generateTerrain(result, 0, centerY, centerX, centerY, centerX,
                result.size() - 1, 0, result.size() - 1);

        return result;
    }

    public void generateTerrain(ArrayList<ArrayList<Double>> terrain, int tLX,
            int tLY, int tRX, int tRY, int bRX, int bRY, int bLX, int bLY) {
        if (tRX - tLX > 1 && bRX - bLX > 1 && bLY - tLY > 1 && bRY - tRY > 1) {
            int tX = (tLX + tRX) / 2;
            int tY = tLY;

            int rX = tRX;
            int rY = (tLY + bLY) / 2;

            int bX = (bLX + bRX) / 2;
            int bY = bLY;

            int lX = tLX;
            int lY = (tLY + bLY) / 2;

            int mX = (lX + rX) / 2;
            int mY = (tY + bY) / 2;

            double area = (tRX - tLX) * (bLY - tLY);

            double t = this.nudge(area)
                    + (terrain.get(tLY).get(tLX) + terrain.get(tRY).get(tRX))
                            / 2;
            double r = this.nudge(area)
                    + (terrain.get(tRY).get(tRX) + terrain.get(bRY).get(bRX))
                            / 2;
            double b = this.nudge(area)
                    + (terrain.get(bLY).get(bLX) + terrain.get(bRY).get(bRX))
                            / 2;
            double l = this.nudge(area)
                    + (terrain.get(tLY).get(tLX) + terrain.get(bLY).get(bLX))
                            / 2;
            double m = this.nudge(area) + (terrain.get(tLY).get(tLX)
                    + terrain.get(tRY).get(tRX) + terrain.get(bRY).get(bRX)
                    + terrain.get(bLY).get(bLX)) / 4;

            int minHeight = -30;

            t = Math.max(Math.min(this.maxHeight, t), minHeight);
            r = Math.max(Math.min(this.maxHeight, r), minHeight);
            b = Math.max(Math.min(this.maxHeight, b), minHeight);
            l = Math.max(Math.min(this.maxHeight, l), minHeight);
            m = Math.max(Math.min(this.maxHeight, m), minHeight);

            if (terrain.get(tY).get(tX) == 0) {
                terrain.get(tY).set(tX, t);
            }
            if (terrain.get(rY).get(rX) == 0) {
                terrain.get(rY).set(rX, r);
            }
            if (terrain.get(bY).get(bX) == 0) {
                terrain.get(bY).set(bX, b);
            }
            if (terrain.get(lY).get(lX) == 0) {
                terrain.get(lY).set(lX, l);
            }
            if (terrain.get(mY).get(mX) == 0) {
                terrain.get(mY).set(mX, m);
            }

            this.generateTerrain(terrain, tLX, tLY, tX, tY, mX, mY, lX, lY);
            this.generateTerrain(terrain, tX, tY, tRX, tRY, rX, rY, mX, mY);
            this.generateTerrain(terrain, mX, mY, rX, rY, bRX, bRY, bX, bY);
            this.generateTerrain(terrain, lX, lY, mX, mY, bX, bY, bLX, bLY);
        }
    }

    public ArrayList<ArrayList<Cell>> generateCells(
            ArrayList<ArrayList<Double>> heights) {
        ArrayList<ArrayList<Cell>> results = new ArrayList<ArrayList<Cell>>();
        for (int i = 0; i < heights.size(); i += 1) {
            ArrayList<Cell> cellRow = new ArrayList<Cell>();
            for (int j = 0; j < heights.get(i).size(); j += 1) {
                if (heights.get(i).get(j) <= 0) {
                    cellRow.add(new OceanCell(j, i));
                }
                else {
                    cellRow.add(new Cell(heights.get(i).get(j), j, i));
                }
            }
            results.add(cellRow);
        }
        return results;
    }

    RandomTerrainIslandGenerator(int maxHeight) {
        super(maxHeight);
    }
}

abstract class Target {
    Cell link;
    
    Target(Cell link) {
        this.link = link;
    }
    
    WorldImage drawInto(WorldImage world) {
        WorldImage empty = new PhantomImage(new EmptyImage(), Cell.CELLSIZE * (AIslandGenerator.ISLAND_SIZE+1), Cell.CELLSIZE * (AIslandGenerator.ISLAND_SIZE+1));
        WorldImage onEmpty = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, empty, this.link.x * Cell.CELLSIZE, this.link.y * Cell.CELLSIZE, this.draw());
        return new OverlayImage(onEmpty, world);    
    }
    
    abstract WorldImage draw();
    
    // check if this target is alive
    boolean isAlive() {
        return !this.link.isFlooded;
    }
    
    // check if a player is on this target. If he isn't, add this item to the given list
    IList<Target> pickup(Player player, IList<Target> current) {
        if (player.link != this.link) {
            current = new Cons<Target>(this, current);
        }
        return current;
    }
}

class PieceTarget extends Target {
    PieceTarget(Cell link) {
        super(link);
    }
    
    WorldImage draw() {
        return new CircleImage(4, OutlineMode.SOLID, Color.MAGENTA);
    }
}

class HelicopterTarget extends Target {
    HelicopterTarget(Cell link) {
        super(link);
    }
    
    WorldImage draw() {
        return new CircleImage(4, OutlineMode.SOLID, Color.ORANGE);
    }    
}

class Player {
    Cell link;
    
    Player(Cell link) {
        this.link = link;
    }
    
    WorldImage draw() {
        return new RectangleImage(8, 8, OutlineMode.SOLID, Color.BLACK);
    }
    
    WorldImage drawInto(WorldImage world) {
        WorldImage empty = new PhantomImage(new EmptyImage(), Cell.CELLSIZE * (AIslandGenerator.ISLAND_SIZE+1), Cell.CELLSIZE * (AIslandGenerator.ISLAND_SIZE+1));
        WorldImage onEmpty = new OverlayOffsetAlign(AlignModeX.LEFT, AlignModeY.TOP, empty, this.link.x * Cell.CELLSIZE, this.link.y * Cell.CELLSIZE, this.draw());
        return new OverlayImage(onEmpty, world);
    }
    
    void handleKey(String key) {
        switch(key) {
        case "up":
            this.moveUp();
            break;
            
        case "down":
            this.moveDown();
            break;
            
        case "right":
            this.moveRight();
            break;
            
        case "left":
            this.moveLeft();
            break;
            
        default:
            break;
        }
    }
    
    // check if the move is legal
    boolean isLegalMove(Cell next) {
        return !next.isFlooded;
    }
    
    // Move player up
    // EFFECT: modifies link
    void moveUp() {
        if (this.isLegalMove(this.link.top)) {
            this.link = this.link.top;
        }
    }
    
    // Move player down
    // EFFECT: modifies link
    void moveDown() {
        if (this.isLegalMove(this.link.bottom)) {
            this.link = this.link.bottom;
        }
    }
    
    // Move player left
    // EFFECT: modifies link
    void moveLeft() {
        if (this.isLegalMove(this.link.left)) {
            this.link = this.link.left;
        }
    }
    
    // Move player right
    // EFFECT: modifies link
    void moveRight() {
        if (this.isLegalMove(this.link.right)) {
            this.link = this.link.right;
        }
    }
    
    // check if the player is alive
    boolean isAlive() {
        return !this.link.isFlooded;
    }
}

class ForbiddenIslandWorld extends World {
    IList<Cell> board; // All the cells of the game,
                       // including the ocean
    int waterHeight; // the current height of the ocean

    // the maximum height of the cells
    int maxHeight;

    // Tick counter
    int tick;
    
    // Player
    Player player;
    
    // Items
    IList<Target> items;
    
    // Helicopter
    HelicopterTarget helicopter;

    // draw the world
    public WorldScene makeScene() {
        WorldScene scene = new WorldScene(
                (AIslandGenerator.ISLAND_SIZE + 1) * 10,
                (AIslandGenerator.ISLAND_SIZE + 1) * 10);
        scene.placeImageXY(this.draw(),
                (int) ((AIslandGenerator.ISLAND_SIZE / 2.0) * 10) + 5,
                (int) ((AIslandGenerator.ISLAND_SIZE / 2.0) * 10) + 5);
        return scene;
    }

    // handle ticking
    public void onTick() {
        this.tick = (this.tick + 1) % 10;
        if (this.tick == 0) {
            this.waterHeight += 1;
            this.flood();
        }
        
        // check game state
        this.worldEnds();
        
        // check collisions
        this.checkCollisions();
    }
    
    public WorldEnd worldEnds() {
        if (this.isOver()) {
            return new WorldEnd(true, this.makeAFinalScene("You lose"));
        }
        return new WorldEnd(false, this.makeScene());
    }
    
    // handle keys
    public void onKeyEvent(String key) {
        System.out.println(key);
        this.player.handleKey(key);
    }
    
    // get a random non-flooded cell from the list of cells
    Cell getRandomDry() {
        int rand = (int)(Math.random() * (AIslandGenerator.ISLAND_SIZE + 1) * (AIslandGenerator.ISLAND_SIZE + 1));
        
        while (this.board.get(rand).isFlooded) {
            rand = (int)(Math.random() * (AIslandGenerator.ISLAND_SIZE + 1) * (AIslandGenerator.ISLAND_SIZE + 1));
        }
        
        return this.board.get(rand);
    }
    
    // place items in the world
    // EFFECT: initializes the targets
    void createTargets() {
        IList<Target> targets = new Empty<Target>();
        
        for(int i = 0; i < 3; i++) {
            targets = new Cons<Target>(new PieceTarget(this.getRandomDry()), targets);
        }
        this.items = targets;
    }
    
    // place player in the world
    // EFFECT: initializes the player
    void createPlayer() {
        this.player = new Player(this.getRandomDry());
    }
    
    // place helicopter
    // EFFECT: initializes helicopter
    void createHelicopter() {
        this.helicopter = new HelicopterTarget(this.getRandomDry());
    }

    // create a new ForbiddenIslandWorld with the given AIslandGenerator
    ForbiddenIslandWorld(AIslandGenerator gen) {
        this.board = gen.generateTerrain();
        this.waterHeight = 0;
        this.tick = 0;
        this.maxHeight = gen.maxHeight;
        
        this.createTargets();
        this.createPlayer();
        this.createHelicopter();
    }

    // draw this world
    WorldImage draw() {
        WorldImage result = new EmptyImage();
        ArrayList<WorldImage> rows = new ArrayList<WorldImage>();

        int idx = 0;
        for (Cell cell : this.board) {
            if (idx == 0) {
                rows.add(new EmptyImage());
            }
            rows.set(rows.size() - 1, new BesideImage(rows.get(rows.size() - 1),
                    cell.draw(this.waterHeight, this.maxHeight)));
            idx += 1;
            idx = idx % (AIslandGenerator.ISLAND_SIZE + 1);
        }

        for (WorldImage image : rows) {
            result = new AboveImage(result, image);
        }
        
        for (Target target : this.items) {
            result = target.drawInto(result);
        }
        
        result = this.helicopter.drawInto(result);
        result = this.player.drawInto(result);
        
        return result;
    }
    
    // draw the last state of the world
    public WorldScene makeAFinalScene(String msg) {
        WorldImage text = new TextImage(msg, 30, Color.BLACK);
        
        WorldScene scene = new WorldScene(
                (AIslandGenerator.ISLAND_SIZE + 1) * 10,
                (AIslandGenerator.ISLAND_SIZE + 1) * 10);
        
        scene.placeImageXY(text, 300, 300);
        
        return scene;
    }

    // flood the world
    // EFFECT: modifies the board
    void flood() {
        for (Cell cell : this.board) {
            if (cell.left.isFlooded || cell.right.isFlooded
                    || cell.top.isFlooded || cell.bottom.isFlooded) {
                cell.flood(this.waterHeight);
            }
        }
    }

    // flood the world with the given water height
    // EFFECT: modifies the board
    void flood(int waterHeight) {
        for (Cell cell : this.board) {
            if (cell.left.isFlooded || cell.right.isFlooded
                    || cell.top.isFlooded || cell.bottom.isFlooded) {
                cell.flood(waterHeight);
            }
        }
    }
    
    // update the targets to remove the ones that the player has landed on
    void checkCollisions() {
        IList<Target> res = new Empty<Target>();
        for(Target t : this.items) {
            res = t.pickup(this.player, res);
        }
        
        this.items = res;
    }
    
    // check if we have lost
    boolean isOver() {
        boolean res = true;
        for (Target t : this.items) {
            res = res && t.isAlive();
        }
        
        res = res && this.player.isAlive() && this.helicopter.isAlive();
        return !res;
    }
}

class ExamplesIslandGame {
    AIslandGenerator mountainGen = new MountainIslandGenerator();
    AIslandGenerator randomGen = new RandomIslandGenerator();
    AIslandGenerator randomTerrainGen = new RandomTerrainIslandGenerator(128);
    ForbiddenIslandWorld worldMountain;
    ForbiddenIslandWorld worldRandom;
    ForbiddenIslandWorld worldTerrain;

    void initializeIslands() {
        this.worldMountain = new ForbiddenIslandWorld(mountainGen);
        this.worldRandom = new ForbiddenIslandWorld(randomGen);
        this.worldTerrain = new ForbiddenIslandWorld(randomTerrainGen);
    }

    // test manhattan distance
    void testManhattanDistance(Tester t) {
        this.initializeIslands();
        t.checkExpect(mountainGen.manhattanDistance(20, 30, 60, 70), 80.0);
        t.checkExpect(mountainGen.manhattanDistance(30, 10, 10, 30), 40.0);
        t.checkExpect(mountainGen.manhattanDistance(10, 10, 20, 20), 20.0);
        t.checkExpect(mountainGen.manhattanDistance(60, 10, 50, 60), 60.0);
        t.checkExpect(mountainGen.manhattanDistance(60, 10, 10, 20), 60.0);
        t.checkExpect(mountainGen.manhattanDistance(50, 60, 30, 30), 50.0);
    }

    // test height generation
    void testGenerateHeight(Tester t) {
        t.checkExpect(mountainGen.generateHeights().get(61).get(43), -8.0);
        t.checkExpect(mountainGen.generateHeights().get(18).get(54), -4.0);
        t.checkExpect(mountainGen.generateHeights().get(46).get(26), 12.0);
        t.checkExpect(mountainGen.generateHeights().get(36).get(7), 3.0);
        t.checkExpect(mountainGen.generateHeights().get(41).get(55), 0.0);
        t.checkExpect(
                mountainGen.generateCells(this.mountainGen.generateHeights())
                        .get(5).get(13).height,
                0.0);
        t.checkExpect(
                mountainGen.generateCells(this.mountainGen.generateHeights())
                        .get(8).get(58).height,
                0.0);
        t.checkExpect(
                mountainGen.generateCells(this.mountainGen.generateHeights())
                        .get(1).get(33).height,
                0.0);
        t.checkExpect(
                mountainGen.generateCells(this.mountainGen.generateHeights())
                        .get(36).get(10).height,
                6.0);
        t.checkExpect(
                mountainGen.generateCells(this.mountainGen.generateHeights())
                        .get(56).get(10).height,
                0.0);

        t.checkExpect(randomGen.generateHeights().get(54).get(45) >= 0, true);
        t.checkExpect(randomGen.generateHeights().get(2).get(44) >= 0, true);
        t.checkExpect(randomGen.generateHeights().get(48).get(39) >= 0, true);
        t.checkExpect(randomGen.generateHeights().get(22).get(64) >= 0, true);
        t.checkExpect(randomGen.generateCells(randomGen.generateHeights())
                .get(36).get(25).height > 0, true);
        t.checkExpect(randomGen.generateCells(this.randomGen.generateHeights())
                .get(51).get(51).height > 0, false);
        t.checkExpect(randomGen.generateCells(this.randomGen.generateHeights())
                .get(58).get(9).height > 0, false);
        t.checkExpect(randomGen.generateCells(this.randomGen.generateHeights())
                .get(2).get(57).height > 0, false);
        t.checkExpect(randomGen.generateCells(this.randomGen.generateHeights())
                .get(24).get(54).height > 0, true);
        t.checkExpect(randomGen.generateCells(this.randomGen.generateHeights())
                .get(26).get(43).height > 0, true);
        t.checkExpect(randomGen.generateCells(this.randomGen.generateHeights())
                .get(41).get(23).height > 0, true);
    }

    // test flooding
    void testFlood(Tester t) {
        // test flooding on mountain terrain
        
        //                                    y  *  x
        worldMountain.flood(9);
        t.checkExpect(worldMountain.board.get(12 * 29).isFlooded, true);
        t.checkExpect(worldMountain.board.get(37 * 12).isFlooded, true);
        t.checkExpect(worldMountain.board.get(23 * 39).isFlooded, true);
        
        worldMountain.flood(11);
        t.checkExpect(worldMountain.board.get(58 * 51).isFlooded, false);
        t.checkExpect(worldMountain.board.get(24 * 6).isFlooded, true);
        t.checkExpect(worldMountain.board.get(53 * 2).isFlooded, true);
        
        worldMountain.flood(16);
        t.checkExpect(worldMountain.board.get(49 * 38).isFlooded, false);
        t.checkExpect(worldMountain.board.get(27 * 31).isFlooded, true);
        t.checkExpect(worldMountain.board.get(50 * 3).isFlooded, true);

        worldMountain.flood(31);
        t.checkExpect(worldMountain.board.get(14 * 55).isFlooded, true);
        t.checkExpect(worldMountain.board.get(49 * 2).isFlooded, true);
        t.checkExpect(worldMountain.board.get(63 * 33).isFlooded, true);
        t.checkExpect(worldMountain.board.get(3 * 34).isFlooded, true);
        t.checkExpect(worldMountain.board.get(42 * 57).isFlooded, true);
        t.checkExpect(worldMountain.board.get(36 * 29).isFlooded, true);
        
        worldMountain.flood(0);
    }

    // test drawing cells
    void testCellDraws(Tester t) {
        Cell cell = new Cell(10, 10, 10);
        cell.isFlooded = true;
        t.checkExpect(cell.draw(20, 64),
                new RectangleImage(10, 10, OutlineMode.SOLID,
                        cell.mix(
                                new Color(0.0f, 0.0f, 1.0f), new Color(0.0f,
                                        0.35f, 0.5f),
                        Math.min(Math.sqrt((20 - cell.height) / 64), 1.0f))));
        cell.isFlooded = false;
        t.checkExpect(cell.draw(25, 128),
                new RectangleImage(10, 10, OutlineMode.SOLID,
                        cell.mix(Color.red, new Color(0.25f, 0.5f,
                                0.0f),
                        Math.min(Math.sqrt((25 - cell.height) / 128), 1.0f))));
        t.checkExpect(cell.draw(8, 128),
                new RectangleImage(10, 10, OutlineMode.SOLID,
                        cell.mix(Color.white, new Color(0.0f, 0.5f, 0.0f),
                                (cell.height - 8) / 128)));
        OceanCell oCell = new OceanCell(10, 10);
        t.checkExpect(oCell.draw(100, 128),
                new RectangleImage(10, 10, OutlineMode.SOLID, Color.BLUE));
    }
}

class ExamplesPlay {
    AIslandGenerator mountainGen = new MountainIslandGenerator();
    AIslandGenerator randomGen = new RandomIslandGenerator();
    AIslandGenerator randomTerrainGen = new RandomTerrainIslandGenerator(128);
    ForbiddenIslandWorld worldMountain;
    ForbiddenIslandWorld worldRandom;
    ForbiddenIslandWorld worldTerrain;

    void initializeIslands() {
        this.worldMountain = new ForbiddenIslandWorld(mountainGen);
        this.worldRandom = new ForbiddenIslandWorld(randomGen);
        this.worldTerrain = new ForbiddenIslandWorld(randomTerrainGen);
    }
    
    
    void testGame(Tester t) {
        this.initializeIslands();
        this.worldTerrain.bigBang(Cell.CELLSIZE*(AIslandGenerator.ISLAND_SIZE + 1), Cell.CELLSIZE*(AIslandGenerator.ISLAND_SIZE + 1), .016);
    }
}
