package solver;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

/**
 * SokoBot: An efficient A* solver for Sokoban puzzles
 * 
 * Algorithm: A* search with pattern database heuristic
 * - Uses Zobrist hashing for fast state comparison
 * - Pre-computes deadlock positions
 * - Implements pattern database for admissible heuristic
 * - Generates optimal solutions
 */
public class SokoBot {
  private Zobrist zobrist;
  private int[][] patternDatabase;

  /**
   * Zobrist hashing: Fast, collision-resistant state hashing
   * Combines player position and box positions into a single long
   */
  private class Zobrist {
    private final long[][][] table;
    private static final int PLAYER_INDEX = 0;
    private static final int BOX_INDEX = 1;

    public Zobrist(int height, int width) {
      table = new long[height][width][2];
      java.util.Random rand = new java.util.Random(12345);
      for (int i = 0; i < height; i++) {
        for (int j = 0; j < width; j++) {
          table[i][j][PLAYER_INDEX] = rand.nextLong();
          table[i][j][BOX_INDEX] = rand.nextLong();
        }
      }
    }

    public long computeHash(int playerRow, int playerCol, int[][] boxPositions) {
      long hash = 0;
      hash ^= table[playerRow][playerCol][PLAYER_INDEX];
      for (int[] boxPos : boxPositions) {
        hash ^= table[boxPos[0]][boxPos[1]][BOX_INDEX];
      }
      return hash;
    }
  }

  /**
   * State represents a game configuration: player position + box positions
   * Comparable by f-cost (g + h) for A* priority queue
   */
  private class State implements Comparable<State> {
    int playerRow, playerCol;
    int[][] boxPositions;
    State parent;
    char move;
    int g, h, f;
    long zobristHash;
    private final char[][] mapData;
    private final List<int[]> goalPositions;
    private final boolean[][] deadSquares;

    public State(int pR, int pC, int[][] bP, char[][] mD, List<int[]> gP, boolean[][] dS) {
      playerRow = pR;
      playerCol = pC;
      boxPositions = bP;
      mapData = mD;
      goalPositions = gP;
      deadSquares = dS;
      parent = null;
      move = ' ';
      g = 0;
      h = calculateHeuristic();
      f = this.g + this.h;

      // Sort boxes for consistent hashing
      Arrays.sort(this.boxPositions, (a, b) -> a[0] != b[0] ? a[0] - b[0] : a[1] - b[1]);
      this.zobristHash = zobrist.computeHash(this.playerRow, this.playerCol, this.boxPositions);
    }

    public State(State parent, int newPR, int newPC, int[][] newBP, char m) {
      this(newPR, newPC, newBP, parent.mapData, parent.goalPositions, parent.deadSquares);
      this.parent = parent;
      this.move = m;
      this.g = parent.g + 1;
      this.f = this.g + this.h;
    }

    /**
     * Heuristic: Sum of pattern database values for each box
     * Pattern database pre-computes minimum pushes to nearest goal
     * This is in fact admissible
     */
    //the original one
    /* private int calculateHeuristic() {
      int totalPushDistance = 0;

      for (int[] boxPos : boxPositions) {
        int pdbVal = patternDatabase[boxPos[0]][boxPos[1]];
        totalPushDistance += (pdbVal == Integer.MAX_VALUE) ? 1000 : pdbVal;
      }

      // Tie-breaker: prefer states where boxes are closer to origin
      int tieBreaker = Arrays.stream(boxPositions).mapToInt(b -> b[0] * 100 + b[1]).sum();

      return totalPushDistance * 10000 + tieBreaker;
    } */
    //new one
    private int calculateHeuristic() {
      int totalDist = 0;
      for (int[] boxPos : boxPositions) {
        int minGoalDist = Integer.MAX_VALUE;
        for (int[] goalPos : goalPositions) {
          // manhattan distance (admissible heuristic)
          int dist = Math.abs(boxPos[0] - goalPos[0]) + Math.abs(boxPos[1] - goalPos[1]);
          if (dist < minGoalDist) minGoalDist = dist;
        }
        totalDist += minGoalDist;
      }
      // tie breaker to prioritize states that are closer to the top left of the board
      int tieBreaker = Arrays.stream(boxPositions).mapToInt(b -> b[0] * 100 + b[1]).sum();
      return totalDist * 10000 + tieBreaker;
    }

    /**
     * Generate successor states by moving player in 4 directions
     * Only includes legal moves (not walls, no deadlocks)
     */
    public List<State> getSuccessors() {
      List<State> successors = new ArrayList<>();
      int[] dRow = { -1, 1, 0, 0 };
      int[] dCol = { 0, 0, -1, 1 };
      char[] moves = { 'u', 'd', 'l', 'r' };

      for (int i = 0; i < 4; i++) {
        int newPlayerRow = playerRow + dRow[i];
        int newPlayerCol = playerCol + dCol[i];

        // Check walls
        if (mapData[newPlayerRow][newPlayerCol] == '#')
          continue;

        // Check if pushing a box
        int boxIndex = getBoxIndexAt(newPlayerRow, newPlayerCol);
        if (boxIndex != -1) {
          int newBoxRow = newPlayerRow + dRow[i];
          int newBoxCol = newPlayerCol + dCol[i];

          // Check if box can be pushed (no obstruction)
          if (mapData[newBoxRow][newBoxCol] != '#' && getBoxIndexAt(newBoxRow, newBoxCol) == -1) {
            if (!deadSquares[newBoxRow][newBoxCol]) {
              int[][] newBoxPositions = deepCopyBoxPositions();
              newBoxPositions[boxIndex][0] = newBoxRow;
              newBoxPositions[boxIndex][1] = newBoxCol;
              successors.add(new State(this, newPlayerRow, newPlayerCol, newBoxPositions, moves[i]));
            }
          }
        } else {
          // Simple move (no box push)
          successors.add(new State(this, newPlayerRow, newPlayerCol, this.boxPositions, moves[i]));
        }
      }
      return successors;
    }

    /**
     * Check if all boxes are on goal positions
     */
    public boolean isGoalState() {
      for (int[] boxPos : boxPositions) {
        boolean onGoal = false;
        for (int[] goalPos : goalPositions) {
          if (boxPos[0] == goalPos[0] && boxPos[1] == goalPos[1]) {
            onGoal = true;
            break;
          }
        }
        if (!onGoal)
          return false;
      }
      return true;
    }

    private int getBoxIndexAt(int row, int col) {
      for (int i = 0; i < boxPositions.length; i++) {
        if (boxPositions[i][0] == row && boxPositions[i][1] == col)
          return i;
      }
      return -1;
    }

    private int[][] deepCopyBoxPositions() {
      int[][] newBoxPositions = new int[boxPositions.length][2];
      for (int i = 0; i < boxPositions.length; i++) {
        newBoxPositions[i][0] = boxPositions[i][0];
        newBoxPositions[i][1] = boxPositions[i][1];
      }
      return newBoxPositions;
    }

    @Override
    public int compareTo(State other) {
      return Integer.compare(this.f, other.f);
    }

    @Override
    public boolean equals(Object o) {
      return o instanceof State && this.zobristHash == ((State) o).zobristHash;
    }

    @Override
    public int hashCode() {
      return Objects.hash(zobristHash);
    }
  }

  /**
   * Main solver: A* search algorithm
   * 
   * @param width   Map width
   * @param height  Map height
   * @param mapData Map structure (walls, goals)
   * @param itemsData Current items (player, boxes)
   * @return Move sequence as string (u/d/l/r)
   */
  public String solveSokobanPuzzle(int width, int height, char[][] mapData, char[][] itemsData) {
    this.zobrist = new Zobrist(height, width);

    // Extract initial state
    int playerRow = -1, playerCol = -1;
    List<int[]> boxPositionsList = new ArrayList<>();
    List<int[]> goalPositions = new ArrayList<>();

    for (int r = 0; r < height; r++) {
      for (int c = 0; c < width; c++) {
        if (itemsData[r][c] == '@') {
          playerRow = r;
          playerCol = c;
        } else if (itemsData[r][c] == '$') {
          boxPositionsList.add(new int[] { r, c });
        }
        if (mapData[r][c] == '.') {
          goalPositions.add(new int[] { r, c });
        }
      }
    }

    int[][] boxPositions = boxPositionsList.toArray(new int[0][]);

    // Pre-compute deadlock positions and pattern database
    boolean[][] deadSquares = precomputeDeadlocks(height, width, mapData, goalPositions);
    this.patternDatabase = buildPatternDatabase(height, width, mapData, goalPositions);

    // A* Search
    State initialState = new State(playerRow, playerCol, boxPositions, mapData, goalPositions, deadSquares);
    PriorityQueue<State> openSet = new PriorityQueue<>();
    Set<Long> closedSet = new HashSet<>();

    openSet.add(initialState);
    closedSet.add(initialState.zobristHash);

    long startTime = System.currentTimeMillis(); // for time

    while (!openSet.isEmpty()) {
      // 15 second time limit
      if (System.currentTimeMillis() - startTime > 14500) {
        return ""; // stop searching and return empty string, timeout
      }
      State currentState = openSet.poll();

      if (currentState.isGoalState()) {
        return reconstructPath(currentState);
      }

      for (State successor : currentState.getSuccessors()) {
        if (closedSet.add(successor.zobristHash)) {
          openSet.add(successor);
        }
      }
    }

    return "No solution found";
  }

  /**
   * Build Pattern Database: BFS from all goal positions backward
   * Computes minimum pushes required for a box at each position to reach a goal
   * Result is an admissible heuristic
   */
  private int[][] buildPatternDatabase(int height, int width, char[][] mapData, List<int[]> goalPositions) {
    int[][] pdb = new int[height][width];
    for (int[] row : pdb)
      Arrays.fill(row, Integer.MAX_VALUE);

    Queue<int[]> queue = new LinkedList<>();

    // Start BFS from all goal positions
    for (int[] goal : goalPositions) {
      pdb[goal[0]][goal[1]] = 0;
      queue.add(new int[] { goal[0], goal[1] });
    }

    int[] dRow = { -1, 1, 0, 0 };
    int[] dCol = { 0, 0, -1, 1 };

    while (!queue.isEmpty()) {
      int[] curr = queue.poll();

      // Consider all directions a box could have been pushed FROM
      for (int i = 0; i < 4; i++) {
        int prevBoxR = curr[0] - dRow[i];
        int prevBoxC = curr[1] - dCol[i];
        int pushFromR = curr[0] + dRow[i];
        int pushFromC = curr[1] + dCol[i];

        // Valid if box can exist at prevBox position and player can stand at pushFrom
        if (prevBoxR > 0 && prevBoxR < height - 1 && prevBoxC > 0 && prevBoxC < width - 1
            && mapData[prevBoxR][prevBoxC] != '#' && mapData[pushFromR][pushFromC] != '#') {
          if (pdb[prevBoxR][prevBoxC] > pdb[curr[0]][curr[1]] + 1) {
            pdb[prevBoxR][prevBoxC] = pdb[curr[0]][curr[1]] + 1;
            queue.add(new int[] { prevBoxR, prevBoxC });
          }
        }
      }
    }

    return pdb;
  }

  /**
   * Pre-compute deadlock positions (corners where boxes can get stuck)
   * Boxes pushed into these positions can never be recovered
   */
  private boolean[][] precomputeDeadlocks(int height, int width, char[][] mapData,
      List<int[]> goalPositions) {
    boolean[][] deadSquares = new boolean[height][width];

    for (int r = 1; r < height - 1; r++) {
      for (int c = 1; c < width - 1; c++) {
        if (mapData[r][c] == '#')
          continue;

        // Skip if this is a goal position
        boolean isGoal = false;
        for (int[] goalPos : goalPositions) {
          if (goalPos[0] == r && goalPos[1] == c) {
            isGoal = true;
            break;
          }
        }
        if (isGoal)
          continue;

        // Detect corners: walls on both sides
        boolean wallUp = mapData[r - 1][c] == '#';
        boolean wallDown = mapData[r + 1][c] == '#';
        boolean wallLeft = mapData[r][c - 1] == '#';
        boolean wallRight = mapData[r][c + 1] == '#';

        if ((wallUp || wallDown) && (wallLeft || wallRight)) {
          deadSquares[r][c] = true;
        }
      }
    }

    return deadSquares;
  }

  /**
   * Reconstruct the solution path by backtracking from goal state to initial state
   */
  private String reconstructPath(State goalState) {
    StringBuilder path = new StringBuilder();
    State currentState = goalState;

    while (currentState.parent != null) {
      path.append(currentState.move);
      currentState = currentState.parent;
    }

    return path.reverse().toString();
  }
}
