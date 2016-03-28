import java.util.ArrayList;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;

// TODO: Create regular mountain
// TODO: Create diamond island of random heights
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
}

// An OceanCell
class OceanCell extends Cell {
    OceanCell(double height, int x, int y) {
        super(height, x, y);
    }
}

// Represents an Island generally
abstract class Island {
    // Defines an int constant
    static final int ISLAND_SIZE = 64;

    // Cells in the Island
    ArrayList<ArrayList<Cell>> terrain;

    // The distance from the center of the island at which the ocean starts
    // (32 by default)
    int oceanDistance;

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
                        .get(Math.min(cur.x + 1, Island.ISLAND_SIZE));
                bottom = cells.get(Math.min(cur.y + 1, Island.ISLAND_SIZE))
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
    abstract void generateTerrain();

    Island() {
        this.oceanDistance = 32;
    }

    Island(int oceanDistance) {
        this.oceanDistance = oceanDistance;
    }
}

class MountainIsland extends Island {
    // generate the heights of the cells on this mountain island
    public ArrayList<ArrayList<Double>> generateHeights() {
        int centerX = Island.ISLAND_SIZE / 2;
        int centerY = Island.ISLAND_SIZE / 2;

        // initialize the heights of the cells in this island
        ArrayList<ArrayList<Double>> heights = new ArrayList<ArrayList<Double>>();
        // iterate over the rows (Y coordinates)
        for (int i = 0; i < Island.ISLAND_SIZE; i += 1) {
            // Create a temporary ArrayList<Double> for this row
            ArrayList<Double> curRow = new ArrayList<Double>();

            // iterate over the columns (X coordinates)
            for (int j = 0; j < Island.ISLAND_SIZE; j += 1) {
                // create cells with their heights based on Manhattan distance
                curRow.add(((centerY - i) + (centerX - j)) * 1.0);
            }

            // add the current row to the list of heights
            heights.add(curRow);
        }

        return heights;
    }

    // generate the cells for this mountain island based on their heights
    ArrayList<ArrayList<Cell>> generateCells(
            ArrayList<ArrayList<Double>> heights) {
        ArrayList<ArrayList<Cell>> result = new ArrayList<ArrayList<Cell>>();
        for (int i = 0; i < heights.size(); i += 1) {
            ArrayList<Cell> cellRow = new ArrayList<Cell>();
            for (int j = 0; j < heights.get(i).size(); j += 1) {
                double height = heights.get(i).get(j);
                cellRow.add(new Cell(height, j, i));
            }
            result.add(cellRow);
        }
        return result;
    }

    // generate the terrain for this mountain island
    // EFFECT: sets the terrain field to the generated terrain
    public void generateTerrain() {
        this.terrain = this
                .fixNeighbors(this.generateCells(this.generateHeights()));
    }
}

/*
 * class ForbiddenIslandWorld extends World { // All the cells of the game,
 * including the ocean IList<Cell> board; // the current height of the ocean int
 * waterHeight; }
 */

class ExamplesIslandGame {
    Island mountainIsland = new MountainIsland();
    
    void initializeIslands() {
        this.mountainIsland.generateTerrain();
    }
    
    void testIslands(Tester t) {
        this.initializeIslands();
        t.checkExpect(this.mountainIsland, null);
    }
}
