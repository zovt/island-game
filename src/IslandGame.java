import java.util.ArrayList;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// Only checked part 1 methods, so the percentage is off 
// but we tested all for this week :D

// Represents a single square of the game area
class Cell {
    // represents absolute height of this cell, in feet
    double height;
    // In logical coordinates, with the origin at the top-left corner of the
    // screen
    int x, y;
    // the four adjacent cells to this one
    Cell left, top, right, bottom;
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
            return new RectangleImage(10, 10, OutlineMode.SOLID,
                    this.mix(maxFlooded, minFlooded,
                            Math.min(Math.sqrt(
                                    (waterHeight - this.height) / maxHeight),
                            1.0f)));
        }

        if (this.height - waterHeight > 0) {
            return new RectangleImage(10, 10, OutlineMode.SOLID,
                    this.mix(maxNoFlood, minNoFlood,
                            (this.height - waterHeight) / maxHeight));
        } else {
            return new RectangleImage(10, 10, OutlineMode.SOLID,
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
        return new RectangleImage(10, 10, OutlineMode.SOLID, Color.BLUE);
    }

    // flood this oceanCell
    void flood(int waterHeight) {
        // do nothing
    }
}

// Represents an AIsland generally
abstract class AIsland {
    // Defines an int constant
    static final int ISLAND_SIZE = 64;

    // Cells in the AIsland
    ArrayList<ArrayList<Cell>> terrain;

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
                right = cells.get(cur.y)
                        .get(Math.min(cur.x + 1, AIsland.ISLAND_SIZE - 1));
                bottom = cells.get(Math.min(cur.y + 1, AIsland.ISLAND_SIZE - 1))
                        .get(cur.x);

                cur.setNeighbors(left, top, right, bottom);
                resultCurRow.add(cur);
            }
            result.add(resultCurRow);
        }
        return result;
    }

    // generate the terrain
    // EFFECT: sets the terrain field to the generated terrain
    public void generateTerrain() {
        ArrayList<ArrayList<Double>> heights = this.generateHeights();
        ArrayList<ArrayList<Cell>> cells = this.generateCells(heights);
        ArrayList<ArrayList<Cell>> fixedCells = this.fixNeighbors(cells);

        this.terrain = fixedCells;
    }

    // draw the item based on water height
    WorldImage draw(int waterHeight) {
        WorldImage result = new EmptyImage();
        for (ArrayList<Cell> row : this.terrain) {
            WorldImage rowImage = new EmptyImage();
            for (Cell cell : row) {
                rowImage = new BesideImage(rowImage,
                        cell.draw(waterHeight, this.maxHeight));
            }
            result = new AboveImage(result, rowImage);
        }
        return result;
    }

    // flood the island based on the current water height
    // EFFECT: modify the list of cells with the current flooded state
    void flood(int waterHeight) {
        for (ArrayList<Cell> row : this.terrain) {
            for (Cell cell : row) {
                if (cell.left.isFlooded || cell.right.isFlooded
                        || cell.top.isFlooded || cell.bottom.isFlooded) {
                    cell.flood(waterHeight);
                }
            }
        }
    }

    AIsland() {
        this.maxHeight = AIsland.ISLAND_SIZE / 2;
    }

    AIsland(int maxHeight) {
        this.maxHeight = maxHeight;
    }
}

// A Diamond-shaped AIsland
abstract class DiamondIsland extends AIsland {
    // The distance from the center of the island at which the ocean starts
    // (32 by default)
    int oceanDistance;

    // generate the cells for this mountain island based on their heights
    public ArrayList<ArrayList<Cell>> generateCells(
            ArrayList<ArrayList<Double>> heights) {
        int centerX = AIsland.ISLAND_SIZE / 2;
        int centerY = AIsland.ISLAND_SIZE / 2;

        ArrayList<ArrayList<Cell>> result = new ArrayList<ArrayList<Cell>>();

        for (int i = 0; i < heights.size(); i += 1) {
            ArrayList<Cell> cellRow = new ArrayList<Cell>();
            for (int j = 0; j < heights.get(i).size(); j += 1) {
                if (this.manhattanDistance(j, i, centerX,
                        centerY) < this.oceanDistance) {
                    double height = heights.get(i).get(j);
                    cellRow.add(new Cell(height, j, i));
                } else {
                    cellRow.add(new OceanCell(j, i));
                }
            }
            result.add(cellRow);
        }
        return result;
    }

    DiamondIsland() {
        this.oceanDistance = 32;
    }

    DiamondIsland(int oceanDistance) {
        this.oceanDistance = oceanDistance;
    }
}

// A Mountain AIsland
class MountainIsland extends DiamondIsland {
    // generate the heights of the cells on this mountain island
    public ArrayList<ArrayList<Double>> generateHeights() {
        int centerX = AIsland.ISLAND_SIZE / 2;
        int centerY = AIsland.ISLAND_SIZE / 2;

        // initialize the heights of the cells in this island
        ArrayList<ArrayList<Double>> heights = new ArrayList<ArrayList<Double>>();
        // iterate over the rows (Y coordinates)
        for (int i = 0; i <= AIsland.ISLAND_SIZE; i += 1) {
            // Create a temporary ArrayList<Double> for this row
            ArrayList<Double> curRow = new ArrayList<Double>();

            // iterate over the columns (X coordinates)
            for (int j = 0; j <= AIsland.ISLAND_SIZE; j += 1) {
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
class RandomIsland extends DiamondIsland {
    // generate the heights of the cells on this random island
    public ArrayList<ArrayList<Double>> generateHeights() {

        Random r = new Random();

        // initialize the heights of the cells in this island
        ArrayList<ArrayList<Double>> heights = new ArrayList<ArrayList<Double>>();
        // iterate over the rows (Y coordinates)
        for (int i = 0; i <= AIsland.ISLAND_SIZE; i += 1) {
            // Create a temporary ArrayList<Double> for this row
            ArrayList<Double> curRow = new ArrayList<Double>();

            // iterate over the columns (X coordinates)
            for (int j = 0; j <= AIsland.ISLAND_SIZE; j += 1) {
                // create cells with their heights determined randomly from 0 to
                // maxSize
                curRow.add(r.nextInt(this.maxHeight + 1) * 1.0);
            }

            // add the current row to the list of heights
            heights.add(curRow);
        }

        return heights;
    }

    RandomIsland() {
        this.maxHeight = 64;
    }
}

class RandomTerrainIsland extends AIsland {
    // generate the nudge
    double nudge(double area) {
        if (Math.random() <= .32) {
            return -1 * Math.random() * area
                    + (Math.random() * this.maxHeight) / this.maxHeight;
        } else {
            return Math.random() * area
                    + (Math.random() * this.maxHeight) / this.maxHeight;
        }
    }

    // generate the heights of the cells on this random terrain island
    public ArrayList<ArrayList<Double>> generateHeights() {
        // Initialize the arraylist to be IslandSize + 1 columns and rows big
        ArrayList<ArrayList<Double>> result = new ArrayList<ArrayList<Double>>(
                AIsland.ISLAND_SIZE + 1);
        for (int i = 0; i < AIsland.ISLAND_SIZE + 1; i += 1) {
            ArrayList<Double> row = new ArrayList<Double>(
                    AIsland.ISLAND_SIZE + 1);
            for (int j = 0; j < AIsland.ISLAND_SIZE + 1; j += 1) {
                row.add(0d);
            }
            result.add(row);
        }

        // set the center of the arraylist to the max height
        int centerX = (AIsland.ISLAND_SIZE + 1) / 2;
        int centerY = (AIsland.ISLAND_SIZE + 1) / 2;
        result.get(centerY).set(centerX, (double) this.maxHeight);

        // set the edges to height 1
        result.get(0).set(centerX, 1d);
        result.get(AIsland.ISLAND_SIZE - 1).set(centerX, 1d);
        result.get(centerY).set(0, 1d);
        result.get(centerY).set(AIsland.ISLAND_SIZE - 1, 1d);

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

            t = Math.min(this.maxHeight, t);
            r = Math.min(this.maxHeight, r);
            b = Math.min(this.maxHeight, b);
            l = Math.min(this.maxHeight, l);
            m = Math.min(this.maxHeight, m);

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
                } else {
                    cellRow.add(new Cell(heights.get(i).get(j), j, i));
                }
            }
            results.add(cellRow);
        }
        return results;
    }

    RandomTerrainIsland(int maxHeight) {
        super(maxHeight);
    }
}

class ForbiddenIslandWorld extends World { // All the cells of the game,
                                           // including the ocean
    // IList<Cell> board; // the current height of the ocean
    int waterHeight;

    // The island of the world
    AIsland island;

    // Tick counter
    int tick;

    // draw the world
    public WorldScene makeScene() {
        WorldScene scene = new WorldScene(AIsland.ISLAND_SIZE * 10,
                AIsland.ISLAND_SIZE * 10);
        scene.placeImageXY(this.island.draw(waterHeight),
                AIsland.ISLAND_SIZE / 2 * 10, AIsland.ISLAND_SIZE / 2 * 10);
        return scene;
    }

    // handle ticking
    public void onTick() {
        this.tick = (this.tick + 1) % 1;
        if (this.tick == 0) {
            this.waterHeight += 1;
            this.island.flood(waterHeight);
        }
    }

    // create a new ForbiddenIslandWorld with the given AIsland
    ForbiddenIslandWorld(AIsland island) {
        this.island = island;
        this.waterHeight = 0;
        this.tick = 0;
    }
}

class ExamplesIslandGame {
    AIsland mountainIsland = new MountainIsland();
    AIsland randomIsland = new RandomIsland();
    AIsland randomTerrainIsland = new RandomTerrainIsland(128);
    ForbiddenIslandWorld world;

    void initializeIslands() {
        this.mountainIsland.generateTerrain();
        this.randomIsland.generateTerrain();
        this.randomTerrainIsland.generateTerrain();
        this.world = new ForbiddenIslandWorld(randomTerrainIsland);
    }

    void testIslands(Tester t) {
        this.initializeIslands();
        // this.world.bigBang(640, 640, .016);
    }

    // stuff to check manhattanDistance, generateHeights, generateCells, flood,
    // fixNeighbors,
    void testManhattanDistance(Tester t) {
        t.checkExpect(mountainIsland.manhattanDistance(20, 30, 60, 70), 80.0);
        t.checkExpect(mountainIsland.manhattanDistance(30, 10, 10, 30), 40.0);
        t.checkExpect(mountainIsland.manhattanDistance(10, 10, 20, 20), 20.0);
        t.checkExpect(mountainIsland.manhattanDistance(60, 10, 50, 60), 60.0);
        t.checkExpect(mountainIsland.manhattanDistance(60, 10, 10, 20), 60.0);
        t.checkExpect(mountainIsland.manhattanDistance(50, 60, 30, 30), 50.0);
    }

    void testGenerateHeight(Tester t) {
        t.checkExpect(mountainIsland.generateHeights().get(61).get(43), -8.0);
        t.checkExpect(mountainIsland.generateHeights().get(18).get(54), -4.0);
        t.checkExpect(mountainIsland.generateHeights().get(46).get(26), 12.0);
        t.checkExpect(mountainIsland.generateHeights().get(36).get(7), 3.0);
        t.checkExpect(mountainIsland.generateHeights().get(41).get(55), 0.0);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(5)
                .get(13).height, 0.0);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(8)
                .get(58).height, 0.0);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(1)
                .get(33).height, 0.0);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(36)
                .get(10).height, 6.0);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(56)
                .get(10).height, 0.0);

        t.checkExpect(randomIsland.generateHeights().get(54).get(45) >= 0,
                true);
        t.checkExpect(randomIsland.generateHeights().get(2).get(44) >= 0, true);
        t.checkExpect(randomIsland.generateHeights().get(48).get(39) >= 0,
                true);
        t.checkExpect(randomIsland.generateHeights().get(22).get(64) >= 0,
                true);
        t.checkExpect(randomIsland.generateCells(randomIsland.generateHeights())
                .get(36).get(25).height > 0, true);
        t.checkExpect(
                randomIsland.generateCells(this.randomIsland.generateHeights())
                        .get(51).get(51).height > 0,
                false);
        t.checkExpect(
                randomIsland.generateCells(this.randomIsland.generateHeights())
                        .get(58).get(9).height > 0,
                false);
        t.checkExpect(
                randomIsland.generateCells(this.randomIsland.generateHeights())
                        .get(2).get(57).height > 0,
                false);
        t.checkExpect(
                randomIsland.generateCells(this.randomIsland.generateHeights())
                        .get(24).get(54).height > 0,
                true);
        t.checkExpect(
                randomIsland.generateCells(this.randomIsland.generateHeights())
                        .get(26).get(43).height > 0,
                true);
        t.checkExpect(
                randomIsland.generateCells(this.randomIsland.generateHeights())
                        .get(41).get(23).height > 0,
                true);
    }

    void testFlood(Tester t) {
        // test flooding on mountain terrain
        mountainIsland.generateTerrain();
        mountainIsland.flood(11);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(58)
                .get(51).isFlooded, true);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(24)
                .get(6).isFlooded, true);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(53)
                .get(2).isFlooded, true);

        mountainIsland.generateTerrain();
        mountainIsland.flood(31);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(14)
                .get(55).isFlooded, true);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(49)
                .get(2).isFlooded, true);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(63)
                .get(33).isFlooded, true);

        mountainIsland.generateTerrain();
        mountainIsland.flood(16);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(49)
                .get(38).isFlooded, false);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(27)
                .get(31).isFlooded, false);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(50)
                .get(3).isFlooded, true);

        mountainIsland.generateTerrain();
        mountainIsland.flood(9);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(12)
                .get(29).isFlooded, false);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(37)
                .get(12).isFlooded, false);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(23)
                .get(39).isFlooded, false);

        mountainIsland.generateTerrain();
        mountainIsland.flood(31);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(3)
                .get(34).isFlooded, false);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(42)
                .get(57).isFlooded, true);
        t.checkExpect(mountainIsland
                .generateCells(this.mountainIsland.generateHeights()).get(36)
                .get(29).isFlooded, false);
    }
}
