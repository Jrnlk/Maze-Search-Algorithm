import java.util.*;
import tester.*;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;


/* 
 * ////////////////////////////////////////////////////////////////////
 *                      ---- GAME INSTRUCTIONS ---
 *  1. Control the number of cells/vertices using the mHeight and mWidth variables in the 
 *     examples class
 *    1a. For larger boards, you might want to increase the number of ticks per second 
 *        of big bang (found in the first tester test)
 *        
 *  2. After the maze has been sequentially generated press either "D" or "B" on the keyboard
 *     to start solving the maze with depth-first and breadth-first search, respectively
 *     
 *  3. When the maze has been solved, the complete path from start to finish will change to green
 *     and there will be a message below the instructions that tells the user how many moves it
 *     took to solve the entire maze
 *     
 *  4. Press "R" to generate a completely new maze
 *  
 *  
 * ////////////////////////////////////////////////////////////////////
 */

// to represent a single square of the maze 
class Cell { 
  int x;
  int y;

  Color color = Color.LIGHT_GRAY;

  Cell top;
  Cell left;
  Cell right;
  Cell bottom;



  Cell(int x, int y) { 
    this.x = x;
    this.y = y;
  }

  Cell(int x, int y, Color c) { 
    this(x, y);
    this.color = c;
  }

  // draws the cell on the given background at this cell's pos times the displacement
  void drawCell(WorldScene bg, int dis) { 
    WorldImage cellImage = new RectangleImage(dis, dis, OutlineMode.SOLID, this.color)
        .movePinhole(0, 0);
    bg.placeImageXY(cellImage, (this.x * dis) + 50, (this.y * dis) + 50); 
  }

  // string representation of a cell, for testing 
  public String toString() { 
    return "(" + this.x + "," + this.y + ")";
  }

  // changes the color of this cell to the given color
  // EFFECT: changes the color of this cell to the given color
  void changeColor(Color c) { 
    this.color = c;
  }

  // changes the top neighbor
  // EFFECT: mutates the top neighbor to the given cell
  void changeTop(Cell c) { 
    this.top = c;
  }

  // changes the left neighbor
  // EFFECT: mutates the left neighbor to the given cell
  void changeLeft(Cell c) { 
    this.left = c;
  }

  // changes the right neighbor
  // EFFECT: mutates the right neighbor to the given cell
  void changeRight(Cell c) { 
    this.right = c;
  }

  // changes the bottom neighbor
  // EFFECT mutates the bottom neighbor to the given cell
  void changeBottom(Cell c) { 
    this.bottom = c;
  }

  // returns this cell's x coordinate
  int getX() { 
    return this.x;
  }

  // returns this cell's y coordinate
  int getY() { 
    return this.y;
  }

  // returns this cell's color
  Color getColor() { 
    return this.color;
  }
}

// to represent the connections between each maze cell
class Edge { 
  int weight;
  Cell to;
  Cell from;

  Edge(int weight) { 
    this.weight = weight;
  }

  Edge(int weight, Cell to, Cell from) { 
    this(weight);
    this.to = to;
    this.from = from;
  }

  // draws this edge using the given background and cell size
  WorldScene drawEdge(WorldScene bg, int dis) { 
    if (this.to.getX() != this.from.getX()) { 
      // horizontal edge
      WorldImage vLine = new RectangleImage(2, dis, OutlineMode.SOLID, Color.black)
          .movePinhole(0, 0);
      int cellCoordX = (this.to.getX() * dis) + 50;
      int cellCoordY = (this.to.getY() * dis) + 50;
      bg.placeImageXY(vLine, cellCoordX + (dis / 2), cellCoordY);
    }
    else { 
      // vertical edge
      WorldImage hLine = new RectangleImage(dis, 2, OutlineMode.SOLID, Color.black)
          .movePinhole(0, 0);
      int cellCoordX = (this.to.getX() * dis) + 50;
      int cellCoordY = (this.to.getY() * dis) + 50;
      bg.placeImageXY(hLine, cellCoordX, cellCoordY + (dis / 2));
    }

    return bg;
  }

  // string representation of an edge for testing 
  public String toString() { 
    return "To: " + this.to.toString() + " From: " + this.from.toString() + "\n";
  }

  // returns this edge's to cell
  Cell getTo() {
    return this.to;
  }

  // returns this edge's from cell
  Cell getFrom() { 
    return this.from;
  }
}

// to graphically represent the creation and solution of the maze
class MazeWorld extends World {

  // the 2D representation of the maze using cell objects
  ArrayList<ArrayList<Cell>> maze = new ArrayList<ArrayList<Cell>>();

  // all of the possible edges between each of the cells in the maze
  ArrayList<Edge> edges = new ArrayList<Edge>();

  // union/find data structure for kruskal's algo to work efficiently
  HashMap<Cell, Cell> rep = new HashMap<Cell, Cell>();

  // the list of edges created by Kruskal's
  ArrayList<Edge> edgesInTree = new ArrayList<Edge>();

  // a copy of the list of edges
  ArrayList<Edge> edgeCopy = new ArrayList<Edge>();

  // a list containing the cells in the order in which they were traversed by the solving algo
  HashMap<Cell, Cell> visited = new HashMap<Cell, Cell>();

  // the worklist for the DFS or BFS algorithms; taking from the front of back depending on algo
  ArrayList<Cell> worklist = new ArrayList<Cell>();

  // boolean to tell event handlers to pause while the maze is being solved
  boolean searchStarted = false;

  // a string to be displayed only when the maze has been solved
  String endMsg = "";

  // determines which searching algorithm will be used to solve the maze (t = dfs, f = bfs)
  String searchAlgo = "none";

  // number of cells vertically
  int height;

  // number of cells horiztonally
  int width;

  // nummber of vertices in the maze
  int numCells;

  // boolean to determine if the neighbors in the board have been updated after the maze path
  // is created
  boolean setNeighbors = false;

  // cell size, proportional to size of the board
  int displacement;

  // random object for creating random edge weights
  Random rand = new Random();

  MazeWorld(int height, int width) {
    this.height = height;
    this.width = width; 
    numCells = this.height * this.width;

    if (this.height >= 22 || this.width >= 22) { 
      displacement = 12;
    }
    else { 
      displacement = 40;
    }

    this.initBoard(this.numCells); 
    this.initEdges();
    this.edges.sort(new EdgeWeightSort());
    this.initMaze();

    edgeCopy = new ArrayList<Edge>(this.edges);
  }

  // creates all of the cells and edges in the maze and randomly sets edge weights
  void initBoard(int numCells) { 
    // initialize loop variables
    int i = 0;
    int row = 0;
    int col = 0;
    ArrayList<Cell> rows = new ArrayList<Cell>();

    // keep iterating until the number of cells created is equal to the length * width
    while (i < numCells) { 

      while (col < this.width) { 
        // Create the cell at the current row and column
        Cell temp;
        if (row == this.height - 1 && col == this.width - 1) { 
          temp = new Cell(col, row, Color.MAGENTA);
        }
        else if (row == 0 && col == 0) { 
          temp = new Cell(col, row, Color.green);
        }
        else { 
          temp = new Cell(col, row);
        }
        // add the current cell to the current row
        rows.add(temp);
        //System.out.println(rows.toString());
        // increment loop variables
        i++;
        col++;
      }
      // add the entire row to the 2D array and increment loop variables
      ArrayList<Cell> tempRow = new ArrayList<Cell>(rows);
      this.maze.add(tempRow);
      // System.out.println(this.maze.toString());
      rows = new ArrayList<Cell>();
      row++;
      col = 0;
    }
  }

  // creates all of the edges between the vertices
  // EFFECT: changes the list of edges to add the newly created edges
  void initEdges() { 
    // creates every neighboring vertex's edge and assigns random weights
    for (int i = 0; i < this.maze.size(); i++) { 
      for (int j = 0; j < this.maze.get(i).size(); j++) { 
        // create new horizontal edge while there's still another vertex to the right and add to
        // list of edges
        if (j + 1 != this.maze.get(i).size()) { 
          Edge hor = new Edge(this.rand.nextInt(42000), this.maze.get(i).get(j),
              this.maze.get(i).get(j + 1));
          this.edges.add(hor);
        }
        // create new vertical edge while there's still another vertex below and add to list of
        // edges
        if (i + 1 != this.maze.size()) { 
          Edge ver = new Edge(this.rand.nextInt(42000), this.maze.get(i).get(j),
              this.maze.get(i + 1).get(j));
          this.edges.add(ver);
        }
      }
    }
  }

  // Uses Kruskal's algorithm to create a minimum spanning tree in the maze
  // EFFECT: adds edges to the edgesInTree list as the maze gets builts
  void initMaze() { 
    for (int i = 0; i < this.maze.size(); i++) { 
      int rowSize = this.maze.get(i).size();
      for (int j = 0; j < rowSize; j++) { 
        Cell curCell = this.maze.get(i).get(j);
        this.rep.put(curCell, curCell);
      }
    }
  }

  // keeps detecting representatives until the key equals the value in the hashmap
  Cell findFinalRep(Cell key) { 
    Cell result = this.rep.get(key);
    while (result != this.rep.get(result)) { 
      result = this.rep.get(result);
    }
    return this.rep.get(result);
  }

  // sets the neighbors of all of the cells after the maze path has been generated
  void initNeighbors() { 
    for (int i = 0; i < this.edgesInTree.size(); i++) { 
      // all edges in these edges are the path in the maze, so these cells should be neighbors
      Edge curEdge = this.edgesInTree.get(i); 
      Cell curTo = curEdge.getTo(); 
      Cell curFrom = curEdge.getFrom(); 

      if (curTo.getX() < curFrom.getX()) {  
        curTo.changeRight(curFrom); 
        curFrom.changeLeft(curTo);
      }
      if (curTo.getX() > curFrom.getX()) { 
        curTo.changeLeft(curFrom);
        curFrom.changeRight(curTo);
      }
      if (curTo.getY() < curFrom.getY()) { 
        curTo.changeBottom(curFrom);
        curFrom.changeTop(curTo);
      }
      if (curTo.getY() > curFrom.getY()) { 
        curTo.changeTop(curFrom);
        curFrom.changeBottom(curTo);
      }
    }
  }

  @Override
  public WorldScene makeScene() {
    int worldHeight = this.height * displacement + 100;
    int worldWidth = this.width * displacement + 100;
    WorldScene background = new WorldScene(worldWidth, worldHeight);
    for (int i = 0; i < this.maze.size(); i++) { 
      for (int j = 0; j < this.maze.get(0).size(); j++) { 
        this.maze.get(i).get(j).drawCell(background, this.displacement);
      }
    }

    ArrayList<Edge> edgesCopy = new ArrayList<Edge>(this.edges);
    edgesCopy.removeAll(this.edgesInTree);
    for (int i = 0; i < edgesCopy.size(); i++) { 
      edgesCopy.get(i).drawEdge(background, this.displacement);
    }

    WorldImage instructions1 = new TextImage("D for DFS, B for BFS",
        15, Color.black).movePinhole(0,0);
    WorldImage instructions2 = new TextImage("Press R to reset Maze", 15, Color.black)
        .movePinhole(0, 0);
    WorldImage endMsg = new TextImage(this.endMsg, 15, Color.green);

    background.placeImageXY(instructions1, worldWidth / 3 - 30,  worldHeight - 45);
    background.placeImageXY(instructions2, (worldWidth * 2) / 3 + 20, worldHeight - 45);
    background.placeImageXY(endMsg, worldWidth / 2, worldWidth - 28);

    return background;
  } 

  @Override
  public void onTick() { 
    if (this.edgesInTree.size() < this.numCells - 1) { 
      Edge curEdge = this.edgeCopy.remove(0);
      Cell curTo = curEdge.getTo();
      Cell curFrom = curEdge.getFrom();

      Cell representativeTo = this.findFinalRep(curTo);
      Cell representativeFrom = this.findFinalRep(curFrom);
      if (representativeTo != representativeFrom) { 
        this.edgesInTree.add(curEdge);
        this.rep.replace(representativeTo, representativeFrom);
      }
      // otherwise do nothing with the edge -- discard it
    }
    // after maze path is created w/ edges, neighbor cells are connected (but only once)
    else if (!this.setNeighbors) { 
      this.initNeighbors();
      this.setNeighbors = true;
    }

    if (this.setNeighbors && this.searchAlgo.equals("dfs")) { 
      // perform DFS search one cell at a time
      this.worklist.add(this.maze.get(0).get(0));

      if (!this.worklist.isEmpty()) { 
        Cell next = this.worklist.remove(0);

        if (next == this.maze.get(this.height - 1).get(this.width - 1)) { 
          // target cell -- win!          
          this.reconstruct(next);

        }
        else if (this.visited.containsValue(next)) {
          // discard cell -- already processed

        }
        else { 
          // add all valid neighbors of next to the worklist to be processed
          if (next.top != null) { 
            this.worklist.add(0, next.top);
            this.visited.putIfAbsent(next.top, next);
          }
          if (next.right != null) { 
            this.worklist.add(0, next.right);
            this.visited.putIfAbsent(next.right, next);
          }
          if (next.left != null) { 
            this.worklist.add(0, next.left);
            this.visited.putIfAbsent(next.left, next);
          }
          if (next.bottom != null) { 
            this.worklist.add(0, next.bottom);
            this.visited.putIfAbsent(next.bottom, next);
          }
          // add next to the visited list, so that it is not visited again and can be reconstructed
          next.changeColor(Color.cyan);


        }
      }
    }
    else if (this.setNeighbors && this.searchAlgo.equals("bfs")) { 
      // perform BFS search one cell at a time
      this.worklist.add(this.maze.get(0).get(0));

      if (!this.worklist.isEmpty()) { 
        Cell next = this.worklist.remove(0);
        if (next == this.maze.get(this.height  - 1).get(this.width - 1)) { 
          // target cell -- win!          
          this.reconstruct(next);

        }
        else if (this.visited.containsValue(next)) {
          // discard cell -- already processed
        }
        else { 
          // add all valid neighbors of next to the worklist to be processed
          if (next.top != null) { 
            this.worklist.add(next.top);
            this.visited.putIfAbsent(next.top, next);
          }
          if (next.right != null) { 
            this.worklist.add(next.right);
            this.visited.putIfAbsent(next.right, next);
          }
          if (next.left != null) { 
            this.worklist.add(next.left);
            this.visited.putIfAbsent(next.left, next);
          }
          if (next.bottom != null) { 
            this.worklist.add(next.bottom);
            this.visited.putIfAbsent(next.bottom, next);
          }
          next.changeColor(Color.cyan);
        }
      }
    }
  }

  // reconstructs the correct path from the start to finish
  void reconstruct(Cell target) { 
    target.changeColor(Color.green);
    Cell prev = this.visited.get(target);

    while (this.maze.get(0).get(0).getColor() != Color.green) { 
      prev.changeColor(Color.green);
      prev = this.visited.get(prev); 
      this.makeScene();
      this.searchAlgo = "none";
    }
    this.endMsg = "Maze Solved in " + this.visited.size() + " moves!";


  }

  @Override 
  public void onKeyEvent(String key) {
    boolean mazeGenerated = this.setNeighbors;
    if (this.searchStarted) { 
      // do nothing while the maze is being solved

    }
    else if (key.equals("d") && mazeGenerated) { 
      // start depth first search, if the maze has been generated
      this.searchAlgo = "dfs";
      this.searchStarted = true;

    }
    else if (key.equals("b") && mazeGenerated) { 
      // start breadth first seaerch, if the maze has been generated
      this.searchAlgo = "bfs";
      this.searchStarted = true;

    }

    if (key.equals("r")) { 
      // reset and generate new maze       
      this.maze = new ArrayList<ArrayList<Cell>>();      
      this.edges = new ArrayList<Edge>();
      this.rep = new HashMap<Cell, Cell>();
      this.edgesInTree = new ArrayList<Edge>();     
      this.edgeCopy = new ArrayList<Edge>();      
      this.visited = new HashMap<Cell, Cell>();     
      this.worklist = new ArrayList<Cell>();     
      this.searchStarted = false;    
      this.searchAlgo = "none";  
      this.setNeighbors = false;
      this.endMsg = "";

      this.initBoard(this.numCells); 
      this.initEdges();
      this.edges.sort(new EdgeWeightSort());
      this.initMaze();
      edgeCopy = new ArrayList<Edge>(this.edges);

    }
  }
}

// a function object to compare the weights of edges
class EdgeWeightSort implements Comparator<Edge> {

  @Override
  // returns a positive if edge1 weight is greater than edge2 weight, negative if edge2 weight
  // is greater, and 0 if they are equal to compare the edge weight of a given maze edge.
  public int compare(Edge o1, Edge o2) {
    return o1.weight - o2.weight;
  } 
}

// examples of maze cells, edges, and worlds
class ExamplesMazeWorld { 
  ExamplesMazeWorld() { }

  Cell c1 = new Cell(0, 0, Color.green);
  Cell c2 = new Cell(1, 0, Color.LIGHT_GRAY);
  Cell c3 = new Cell(0, 1, Color.LIGHT_GRAY);
  Edge e1 = new Edge(2, c1, c2);
  Edge e2 = new Edge(10, c1, c3);
  Cell c4 = new Cell(1,1, Color.magenta);
  Edge e3 = new Edge(11, c2, c4);
  Edge e4 = new Edge(1, c3, c4);
  Cell c5 = new Cell(2,0);
  Cell c6 = new Cell(0,2);
  Cell c7 = new Cell(2,2);

  int mHeight = 20;
  int mWidth = 20;

  MazeWorld m = new MazeWorld(this.mHeight, this.mWidth);
  MazeWorld m2 = new MazeWorld(2, 2);

  EdgeWeightSort edgeComp = new EdgeWeightSort();

  HashMap<Cell, Cell> testRep1 = new HashMap<Cell, Cell>();
  HashMap<Cell, Cell> testRep2 = new HashMap<Cell, Cell>();


  WorldScene bg = new WorldScene(this.m2.width * this.m2.displacement + 100,
      this.m2.height * this.m2.displacement + 100);

  // instantiating example variables
  void initExamples() { 
    this.c1 = new Cell(0, 0, Color.green);
    this.c2 = new Cell(1, 0);
    this.c3 = new Cell(0, 1);
    this.e1 = new Edge(2, c1, c2);
    this.e2 = new Edge(10, c1, c3);
    c4 = new Cell(1,1, Color.magenta);
    e3 = new Edge(11, c2, c4);
    e4 = new Edge(1, c3, c4);
    c5 = new Cell(2,0);
    c6 = new Cell(0,2);
    c7 = new Cell(2,2);

    this.testRep1.put(c1, c1);
    this.testRep1.put(c2, c2);
    this.testRep1.put(c3, c3);

    this.testRep2.put(c7, c6);
    this.testRep2.put(c6, c5);
    this.testRep2.put(c5, c4);
    this.testRep2.put(c4, c3);
    this.testRep2.put(c3, c3);


    m = new MazeWorld(this.mHeight, this.mWidth);
    m2 = new MazeWorld(2, 2);
    this.edgeComp = new EdgeWeightSort();
    bg = new WorldScene(this.m2.width * this.m2.displacement + 100,
        this.m2.height * this.m2.displacement + 100);
  }

  //visualize the board
  void testFloodWorld(Tester t) {
    int bigBangHeight = this.m.height * this.m.displacement + 100;
    int bigBangWidth = this.m.width * this.m.displacement + 100;
    this.m.bigBang(bigBangWidth, bigBangHeight, 0.000000000000000000000000000000000000000000000001);
  }


  // tests for the initCells method
  void testInitBoard(Tester t) { 
    t.checkExpect(this.m.maze.size(), this.mHeight);
    t.checkExpect(this.m.maze.get(1).size(), this.mWidth);
  }

  // tesets for the initEdges method
  void testInitEdges(Tester t) { 
    t.checkExpect(this.m2.edges.size(), 4);
    t.checkExpect(this.m.edges.size(), 760);
    t.checkExpect(this.m.edges.get(3).weight != this.m.edges.get(5).weight, true);
    t.checkExpect(this.m.edges.get(5).weight != this.m.edges.get(10).weight, true);
  }

  // tests for initMaze method
  void testInitMaze(Tester t) { 
    initExamples();

    t.checkExpect(this.m2.rep.get(this.m2.maze.get(0).get(0)), this.m2.maze.get(0).get(0));
    t.checkExpect(this.m2.rep.get(this.m2.maze.get(0).get(1)), this.m2.maze.get(0).get(1));
  }

  // tests for the initNeighbor method
  void testInitNeighbor(Tester t) { 
    initExamples();

    t.checkExpect(this.m2.maze.get(0).get(0).top, null);
    t.checkExpect(this.m2.maze.get(0).get(0).bottom != null || 
        this.m2.maze.get(0).get(0).right != null, false);

    // need to use kruskal's to get edges in the path, so neighbors can be set
    while (!this.m2.setNeighbors) { 
      this.m2.onTick();
    }
    this.m2.initNeighbors();
    t.checkExpect(this.m2.maze.get(0).get(0).top, null);
    t.checkExpect(this.m2.maze.get(0).get(0).bottom == null && 
        this.m2.maze.get(0).get(0).right == null, false);

  }

  // tests for the onTick method
  void testOnTick(Tester t) { 
    initExamples();
    // number of edges in tree increases after a tick
    int numEdges = this.m2.edgesInTree.size();
    this.m2.onTick();
    t.checkExpect(numEdges < this.m2.edgesInTree.size(), true);

    // number of visited increases after a tick
    initExamples();
    this.m2.searchStarted = false;
    this.m2.setNeighbors = true;
    t.checkExpect(this.m2.maze.get(0).get(0).color, Color.green);
    this.m2.searchAlgo = "dfs";
    this.m2.onTick();
    t.checkExpect(this.m2.maze.get(0).get(0).color, Color.cyan);
  }

  // tests for the onKeyEvent Handler
  void testOnKeyEvent(Tester t) {       
    // testing dfs
    initExamples();
    this.m.searchStarted = false;
    this.m.setNeighbors = true;
    t.checkExpect(this.m.searchAlgo.equals("dfs"), false);
    this.m.onKeyEvent("d");
    t.checkExpect(this.m.searchAlgo.equals("dfs"), true);

    // testing bfs
    initExamples();
    this.m.searchStarted = false;
    this.m.setNeighbors = true;
    t.checkExpect(this.m.searchAlgo.equals("bfs"), false);
    this.m.onKeyEvent("b");
    t.checkExpect(this.m.searchAlgo.equals("bfs"), true);

    // testing reset
    initExamples();
    ArrayList<Edge> edgeList1 = this.m.edgesInTree;
    this.m.searchStarted = false;
    this.m.setNeighbors = true;
    this.m.onKeyEvent("d");
    t.checkExpect(this.m.searchAlgo, "dfs");
    this.m.onKeyEvent("r");
    t.checkExpect(this.m.searchAlgo, "none");
    t.checkExpect(edgeList1 == this.m.edgesInTree, false);
  }

  // tests for find final rep method
  void testFindFinalRep(Tester t) { 
    initExamples();
    this.m2.rep = this.testRep1;
    t.checkExpect(this.m2.findFinalRep(c1), c1);
    t.checkExpect(this.m2.findFinalRep(c3), c3);

    initExamples();
    this.m2.rep = this.testRep2;
    t.checkExpect(this.m2.findFinalRep(c4), c3);
    t.checkExpect(this.m2.findFinalRep(c5), c3);
  }


  // tests for the reconstruct method
  void testReconstruct(Tester t) { 
    initExamples();

    this.m2.searchStarted = false;
    this.m2.setNeighbors = true;
    this.m2.onKeyEvent("d");

    t.checkExpect(this.m2.maze.get(1).get(1).color, Color.magenta);
    t.checkExpect(this.m2.endMsg, "");

    this.m2.reconstruct(this.m2.maze.get(1).get(1));

    t.checkExpect(this.m2.maze.get(1).get(1).color, Color.green);
    t.checkExpect(this.m2.endMsg, "Maze Solved in 0 moves!");
  }

  // tests for the makeScene method
  void testMakeScene(Tester t)  {
    initExamples();

    int disp = 40;
    int worldWidth = disp * 2 + 100;
    int worldHeight = disp * 2 + 100;
    WorldImage cell1 = new RectangleImage(40, 40, OutlineMode.SOLID, this.c1.color);
    WorldImage cell2 = new RectangleImage(40, 40, OutlineMode.SOLID, this.c2.color);
    WorldImage cell3 = new RectangleImage(40, 40, OutlineMode.SOLID, this.c3.color);
    WorldImage cell4 = new RectangleImage(40, 40, OutlineMode.SOLID, this.c4.color);

    ArrayList<Edge> edgesToDraw = new ArrayList<Edge>(this.m2.edges);
    edgesToDraw.removeAll(this.m2.edgesInTree);

    bg.placeImageXY(cell1, this.c1.getX() * 40 + 50, this.c1.getY() * 40 + 50);
    bg.placeImageXY(cell2, this.c2.getX() * 40 + 50, this.c2.getY() * 40 + 50);
    bg.placeImageXY(cell3, this.c3.getX() * 40 + 50, this.c3.getY() * 40 + 50);
    bg.placeImageXY(cell4, this.c4.getX() * 40 + 50, this.c4.getY() * 40 + 50);

    for (int i = 0; i < edgesToDraw.size(); i++) { 
      Edge curEdge = edgesToDraw.get(i);
      if (curEdge.to.getX() != curEdge.from.getX()) { 
        WorldImage line = new RectangleImage(2, 40, OutlineMode.SOLID, Color.black);
        bg.placeImageXY(line, curEdge.to.getX() * disp + 50 + (disp / 2),
            curEdge.to.getY() * disp + 50);
      }
      else { 
        WorldImage line = new RectangleImage(40, 2, OutlineMode.SOLID, Color.black);
        bg.placeImageXY(line, curEdge.to.getX() * disp + 50,
            curEdge.to.getY() * disp + 50 + (disp / 2));
      }
    }

    WorldImage instructions1 = new TextImage("D for DFS, B for BFS",
        15, Color.black).movePinhole(0,0);
    WorldImage instructions2 = new TextImage("Press R to reset Maze", 15, Color.black)
        .movePinhole(0, 0);
    WorldImage endMsg = new TextImage("", 15, Color.green);

    bg.placeImageXY(instructions1, worldWidth / 3 - 30,  worldHeight - 45);
    bg.placeImageXY(instructions2, (worldWidth * 2) / 3 + 20, worldHeight - 45);
    bg.placeImageXY(endMsg, worldWidth / 2, worldWidth - 20);


    t.checkExpect(this.m2.makeScene(), bg);
  }

  // tests for the drawCell method
  void testDrawCell(Tester t) { 
    initExamples();

    this.c1.drawCell(bg, 40);
    WorldScene cellTest1 = new WorldScene(this.m2.width * this.m2.displacement + 100,
        this.m2.height * this.m2.displacement + 100);
    cellTest1.placeImageXY(new RectangleImage(40, 40, OutlineMode.SOLID, Color.green)
        .movePinhole(0, 0), 50, 50);
    t.checkExpect(bg, cellTest1);

    initExamples();

    this.c4.drawCell(bg, 40);
    WorldScene cellTest2 = new WorldScene(this.m2.width * this.m2.displacement + 100,
        this.m2.height * this.m2.displacement + 100);
    cellTest2.placeImageXY(new RectangleImage(40, 40, OutlineMode.SOLID, Color.magenta)
        .movePinhole(0, 0), 40 + 50, 40 + 50);
    t.checkExpect(bg, cellTest2);
  }

  // tests for the drawEdge method
  void testDrawEdge(Tester t) { 
    initExamples();

    this.e1.drawEdge(bg, 40);
    WorldScene edgeTest1 = new WorldScene(this.m2.width * this.m2.displacement + 100,
        this.m2.height * this.m2.displacement + 100);
    edgeTest1.placeImageXY(new RectangleImage(2, 40, OutlineMode.SOLID, Color.black)
        .movePinhole(0, 0), 70, 50);

    t.checkExpect(bg, edgeTest1);

    initExamples();

    this.e3.drawEdge(bg, 40);
    WorldScene edgeTest2 = new WorldScene(this.m2.width * this.m2.displacement + 100,
        this.m2.height * this.m2.displacement + 100);
    edgeTest2.placeImageXY(new RectangleImage(40, 2, OutlineMode.SOLID, Color.black)
        .movePinhole(0, 0), 90, 70);

    t.checkExpect(bg, edgeTest2);

  }

  // tests for the compare method
  void testCompare(Tester t) { 
    t.checkExpect(this.edgeComp.compare(e2, e1), 8);
    t.checkExpect(this.edgeComp.compare(e4, e3), -10);
  }

  // tests for the changeTop method
  void testChangeTop(Tester t) { 
    initExamples();

    t.checkExpect(this.c3.top, null);
    this.c3.changeTop(this.c1);
    t.checkExpect(this.c3.top, this.c1);

  }

  // tests for the changeRight method
  void testChangeRight(Tester t) { 
    initExamples();

    t.checkExpect(this.c1.right, null);
    this.c1.changeRight(this.c2);
    t.checkExpect(this.c1.right, this.c2);

  }

  // tests for the changeLeft method
  void testChangeLeft(Tester t) { 
    initExamples();

    t.checkExpect(this.c2.left, null);
    this.c2.changeLeft(this.c1);
    t.checkExpect(this.c2.left, this.c1);

  }

  // tests for the changeBottom method
  void testChangeBottom(Tester t) { 
    initExamples();

    t.checkExpect(this.c1.bottom, null);
    this.c1.changeBottom(this.c3);
    t.checkExpect(this.c1.bottom, this.c3);

  }

  // tests the getX cells method
  void testGetX(Tester t) { 
    initExamples();

    t.checkExpect(this.c1.getX(), 0);
    t.checkExpect(this.c2.getX(), 1);
    t.checkExpect(this.c5.getX(), 2);
  }

  // tests the getY cells method
  void testGetY(Tester t) { 
    initExamples();

    t.checkExpect(this.c1.getY(), 0);
    t.checkExpect(this.c3.getY(), 1);
    t.checkExpect(this.c6.getY(), 2);
  }

  // tests for the getTo edge method
  void testGetTo(Tester t) { 
    initExamples();

    t.checkExpect(this.e1.getTo(), this.c1);
    t.checkExpect(this.e2.getTo(), this.c1);
    t.checkExpect(this.e3.getTo(), this.c2);
  }

  // tests for the getFrom edge method
  void testGetFrom(Tester t) { 
    initExamples(); 

    t.checkExpect(this.e1.getFrom(), this.c2);
    t.checkExpect(this.e2.getFrom(), this.c3);
    t.checkExpect(this.e3.getFrom(), this.c4);
  }

  // tests for the getColor cell method
  void testGetColor(Tester t) { 
    initExamples();

    t.checkExpect(this.c1.getColor(), Color.green);
    t.checkExpect(this.c2.getColor(), Color.LIGHT_GRAY);
    t.checkExpect(this.c4.getColor(), Color.magenta);
  }

  // tests for the changeColor cell method
  void testChangeColor(Tester t) { 
    initExamples();

    t.checkExpect(this.c1.getColor(), Color.green);
    this.c1.changeColor(Color.black);
    t.checkExpect(this.c1.getColor(), Color.black);

    t.checkExpect(this.c4.getColor(), Color.magenta);
    this.c4.changeColor(Color.cyan);
    t.checkExpect(this.c4.getColor(), Color.cyan);
  }

}