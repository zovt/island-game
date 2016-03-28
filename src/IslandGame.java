import java.util.ArrayList;
import java.util.Random;

import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// TODO: Render code

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
        float red = (float)((a.getRed() * mix)/255 + (b.getRed() * (1 - mix))/255);
        float green = (float)((a.getGreen() * mix)/255 + (b.getGreen() * (1 - mix))/255);
        float blue = (float)((a.getBlue() * mix)/255 + (b.getBlue() * (1 - mix))/255);

        return new Color(red, green, blue);
    }
    
    // draw this cell based on the water height and the maximum height of the island
    public WorldImage draw(int waterHeight, int maxHeight) {
        Color max = Color.white;
        Color even = new Color(0.0f, 0.5f, 0.0f);
        Color min = Color.red;
        
        if (this.height - waterHeight > 0) {
            return new RectangleImage(10, 10, OutlineMode.SOLID,
                    this.mix(max, even, this.height/maxHeight));
        } else {
            return new RectangleImage(10, 10, OutlineMode.SOLID,
                    this.mix(min, even, (waterHeight - this.height)/maxHeight));
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
}

// Represents an Island generally
abstract class Island {
    // Defines an int constant
    static final int ISLAND_SIZE = 64;

    // Cells in the Island
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
                        .get(Math.min(cur.x + 1, Island.ISLAND_SIZE - 1));
                bottom = cells.get(Math.min(cur.y + 1, Island.ISLAND_SIZE - 1))
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
        for(ArrayList<Cell> row : this.terrain) {
            WorldImage rowImage = new EmptyImage();
            for(Cell cell : row) {
                rowImage = new BesideImage(rowImage, cell.draw(waterHeight, this.maxHeight));
            }
            result = new AboveImage(result, rowImage);
        }
        return result;
    }
    
    Island() {
        this.maxHeight = Island.ISLAND_SIZE/2;
    }
    
    Island(int maxHeight) {
        this.maxHeight = maxHeight;
    }
}

// A Diamond-shaped Island
abstract class DiamondIsland extends Island {
    // The distance from the center of the island at which the ocean starts
    // (32 by default)
    int oceanDistance;
    
    // generate the cells for this mountain island based on their heights
    public ArrayList<ArrayList<Cell>> generateCells(
            ArrayList<ArrayList<Double>> heights) {
        int centerX = Island.ISLAND_SIZE / 2;
        int centerY = Island.ISLAND_SIZE / 2;
        
        ArrayList<ArrayList<Cell>> result = new ArrayList<ArrayList<Cell>>();
        
        for (int i = 0; i < heights.size(); i += 1) {
            ArrayList<Cell> cellRow = new ArrayList<Cell>();
            for (int j = 0; j < heights.get(i).size(); j += 1) {
                if (this.manhattanDistance(j, i, centerX, centerY) < this.oceanDistance) {
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

// A Mountain Island
class MountainIsland extends DiamondIsland {
    // generate the heights of the cells on this mountain island
    public ArrayList<ArrayList<Double>> generateHeights() {
        int centerX = Island.ISLAND_SIZE / 2;
        int centerY = Island.ISLAND_SIZE / 2;

        // initialize the heights of the cells in this island
        ArrayList<ArrayList<Double>> heights = new ArrayList<ArrayList<Double>>();
        // iterate over the rows (Y coordinates)
        for (int i = 0; i <= Island.ISLAND_SIZE; i += 1) {
            // Create a temporary ArrayList<Double> for this row
            ArrayList<Double> curRow = new ArrayList<Double>();

            // iterate over the columns (X coordinates)
            for (int j = 0; j <= Island.ISLAND_SIZE; j += 1) {
                // create cells with their heights based on Manhattan distance
                curRow.add(this.maxHeight - this.manhattanDistance(j, i, centerX, centerY));
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
        for (int i = 0; i <= Island.ISLAND_SIZE; i += 1) {
            // Create a temporary ArrayList<Double> for this row
            ArrayList<Double> curRow = new ArrayList<Double>();

            // iterate over the columns (X coordinates)
            for (int j = 0; j <= Island.ISLAND_SIZE; j += 1) {
                // create cells with their heights determined randomly from 0 to maxSize
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

class ForbiddenIslandWorld extends World { // All the cells of the game, including the ocean 
    // IList<Cell> board; // the current height of the ocean 
    int waterHeight;
    
    // The island of the world
    Island island;

    // draw the world
    public WorldScene makeScene() {
        WorldScene scene = new WorldScene(Island.ISLAND_SIZE * 10, Island.ISLAND_SIZE*10);
        scene.placeImageXY(this.island.draw(waterHeight), Island.ISLAND_SIZE/2 * 10, Island.ISLAND_SIZE/2 * 10);
        return scene;
    }
    
    // create a new ForbiddenIslandWorld with the given Island
    ForbiddenIslandWorld(Island island) {
        this.island = island;
        this.waterHeight = 0;
    }
}

class ExamplesIslandGame {
    Island mountainIsland = new MountainIsland();
    Island randomIsland = new RandomIsland();
    ForbiddenIslandWorld world;
    
    void initializeIslands() {
        this.mountainIsland.generateTerrain();
        this.randomIsland.generateTerrain();
        this.world = new ForbiddenIslandWorld(randomIsland);
        this.world.waterHeight = 0;
    }
    
    void testIslands(Tester t) {
        this.initializeIslands();
        this.world.bigBang(640, 640);
    }
}
