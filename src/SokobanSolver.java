import java.util.*;

/**
 * Solveur de Sokoban utilisant l'algorithme A*
 * Résout des grilles de puzzle Sokoban en trouvant le chemin optimal
 */
public class SokobanSolver {

    // Symboles du jeu
    private static final char PLAYER = '@';
    private static final char BOX = '$';
    private static final char WALL = '■';
    private static final char EMPTY = '□';
    private static final char TARGET = 'T';
    private static final char BOX_ON_TARGET = '*';
    private static final char PLAYER_ON_TARGET = '+';

    // Directions: Haut, Bas, Gauche, Droite
    private static final int[][] DIRECTIONS = {{-1,0}, {1,0}, {0,-1}, {0,1}};
    private static final char[] DIR_NAMES = {'H', 'B', 'G', 'D'};

    /**
     * Classe représentant l'état du jeu
     */
    static class State {
        char[][] grid;
        int playerRow, playerCol;
        int gCost; // Coût depuis le début
        int hCost; // Heuristique
        State parent;
        char move;

        public State(char[][] grid, int playerRow, int playerCol) {
            this.grid = copyGrid(grid);
            this.playerRow = playerRow;
            this.playerCol = playerCol;
            this.gCost = 0;
            this.hCost = 0;
            this.parent = null;
            this.move = ' ';
        }

        public int fCost() {
            return gCost + hCost;
        }

        // Copie profonde de la grille
        private char[][] copyGrid(char[][] original) {
            char[][] copy = new char[original.length][original[0].length];
            for (int i = 0; i < original.length; i++) {
                copy[i] = Arrays.copyOf(original[i], original[i].length);
            }
            return copy;
        }

        // Génère une clé unique pour l'état (pour éviter les doublons)
        public String getKey() {
            StringBuilder sb = new StringBuilder();
            sb.append(playerRow).append(",").append(playerCol).append("|");
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    if (grid[i][j] == BOX || grid[i][j] == BOX_ON_TARGET) {
                        sb.append(i).append(",").append(j).append(";");
                    }
                }
            }
            return sb.toString();
        }

        // Vérifie si toutes les caisses sont sur des cibles
        public boolean isGoal() {
            for (int i = 0; i < grid.length; i++) {
                for (int j = 0; j < grid[i].length; j++) {
                    if (grid[i][j] == BOX) return false;
                }
            }
            return true;
        }
    }

    /**
     * Heuristique: Somme des distances de Manhattan entre chaque caisse et sa cible la plus proche
     */
    private static int calculateHeuristic(State state) {
        List<int[]> boxes = new ArrayList<>();
        List<int[]> targets = new ArrayList<>();

        // Collecter positions des caisses et cibles
        for (int i = 0; i < state.grid.length; i++) {
            for (int j = 0; j < state.grid[i].length; j++) {
                if (state.grid[i][j] == BOX) {
                    boxes.add(new int[]{i, j});
                }
                if (state.grid[i][j] == TARGET || state.grid[i][j] == BOX_ON_TARGET
                        || state.grid[i][j] == PLAYER_ON_TARGET) {
                    targets.add(new int[]{i, j});
                }
            }
        }

        // Calcul de la somme des distances minimales
        int totalDistance = 0;
        boolean[] usedTargets = new boolean[targets.size()];

        for (int[] box : boxes) {
            int minDist = Integer.MAX_VALUE;
            int bestTarget = -1;

            for (int t = 0; t < targets.size(); t++) {
                int[] target = targets.get(t);
                int dist = Math.abs(box[0] - target[0]) + Math.abs(box[1] - target[1]);
                if (dist < minDist) {
                    minDist = dist;
                    bestTarget = t;
                }
            }

            totalDistance += minDist;
        }

        return totalDistance;
    }

    /**
     * Détecte si une caisse est dans un deadlock (position irréversible)
     */
    private static boolean isDeadlock(char[][] grid, int boxRow, int boxCol) {
        // Deadlock dans un coin
        boolean wallUp = (boxRow == 0 || grid[boxRow-1][boxCol] == WALL);
        boolean wallDown = (boxRow == grid.length-1 || grid[boxRow+1][boxCol] == WALL);
        boolean wallLeft = (boxCol == 0 || grid[boxRow][boxCol-1] == WALL);
        boolean wallRight = (boxCol == grid[0].length-1 || grid[boxRow][boxCol+1] == WALL);

        // Coin détecté
        if ((wallUp && wallLeft) || (wallUp && wallRight) ||
                (wallDown && wallLeft) || (wallDown && wallRight)) {
            // Vérifier si c'est une cible
            return grid[boxRow][boxCol] == BOX;
        }

        return false;
    }

    /**
     * Génère les états successeurs possibles
     */
    private static List<State> getSuccessors(State current) {
        List<State> successors = new ArrayList<>();

        for (int d = 0; d < DIRECTIONS.length; d++) {
            int[] dir = DIRECTIONS[d];
            int newRow = current.playerRow + dir[0];
            int newCol = current.playerCol + dir[1];

            // Vérifier limites
            if (newRow < 0 || newRow >= current.grid.length ||
                    newCol < 0 || newCol >= current.grid[0].length) {
                continue;
            }

            char cell = current.grid[newRow][newCol];

            // Mouvement vers case vide ou cible
            if (cell == EMPTY || cell == TARGET) {
                State newState = new State(current.grid, newRow, newCol);

                // Mettre à jour grille
                if (current.grid[current.playerRow][current.playerCol] == PLAYER_ON_TARGET) {
                    newState.grid[current.playerRow][current.playerCol] = TARGET;
                } else {
                    newState.grid[current.playerRow][current.playerCol] = EMPTY;
                }

                if (cell == TARGET) {
                    newState.grid[newRow][newCol] = PLAYER_ON_TARGET;
                } else {
                    newState.grid[newRow][newCol] = PLAYER;
                }

                newState.gCost = current.gCost + 1;
                newState.hCost = calculateHeuristic(newState);
                newState.parent = current;
                newState.move = DIR_NAMES[d];
                successors.add(newState);
            }
            // Pousser une caisse
            else if (cell == BOX || cell == BOX_ON_TARGET) {
                int boxNewRow = newRow + dir[0];
                int boxNewCol = newCol + dir[1];

                // Vérifier si on peut pousser
                if (boxNewRow >= 0 && boxNewRow < current.grid.length &&
                        boxNewCol >= 0 && boxNewCol < current.grid[0].length) {

                    char behindBox = current.grid[boxNewRow][boxNewCol];

                    if (behindBox == EMPTY || behindBox == TARGET) {
                        State newState = new State(current.grid, newRow, newCol);

                        // Déplacer joueur
                        if (current.grid[current.playerRow][current.playerCol] == PLAYER_ON_TARGET) {
                            newState.grid[current.playerRow][current.playerCol] = TARGET;
                        } else {
                            newState.grid[current.playerRow][current.playerCol] = EMPTY;
                        }

                        // Position joueur
                        if (cell == BOX_ON_TARGET) {
                            newState.grid[newRow][newCol] = PLAYER_ON_TARGET;
                        } else {
                            newState.grid[newRow][newCol] = PLAYER;
                        }

                        // Nouvelle position caisse
                        if (behindBox == TARGET) {
                            newState.grid[boxNewRow][boxNewCol] = BOX_ON_TARGET;
                        } else {
                            newState.grid[boxNewRow][boxNewCol] = BOX;
                        }

                        // Vérifier deadlock
                        if (!isDeadlock(newState.grid, boxNewRow, boxNewCol)) {
                            newState.gCost = current.gCost + 1;
                            newState.hCost = calculateHeuristic(newState);
                            newState.parent = current;
                            newState.move = DIR_NAMES[d];
                            successors.add(newState);
                        }
                    }
                }
            }
        }

        return successors;
    }

    /**
     * Résout le Sokoban avec A*
     */
    public static Solution solve(char[][] initialGrid) {
        long startTime = System.currentTimeMillis();

        // Trouver position initiale du joueur
        int playerRow = -1, playerCol = -1;
        for (int i = 0; i < initialGrid.length; i++) {
            for (int j = 0; j < initialGrid[i].length; j++) {
                if (initialGrid[i][j] == PLAYER || initialGrid[i][j] == PLAYER_ON_TARGET) {
                    playerRow = i;
                    playerCol = j;
                    break;
                }
            }
            if (playerRow != -1) break;
        }

        State initial = new State(initialGrid, playerRow, playerCol);
        initial.hCost = calculateHeuristic(initial);

        PriorityQueue<State> openSet = new PriorityQueue<>(Comparator.comparingInt(State::fCost));
        Set<String> closedSet = new HashSet<>();

        openSet.add(initial);
        int nodesExplored = 0;

        while (!openSet.isEmpty()) {
            State current = openSet.poll();

            if (current.isGoal()) {
                long endTime = System.currentTimeMillis();
                return new Solution(current, nodesExplored, endTime - startTime);
            }

            String key = current.getKey();
            if (closedSet.contains(key)) continue;
            closedSet.add(key);
            nodesExplored++;

            for (State successor : getSuccessors(current)) {
                if (!closedSet.contains(successor.getKey())) {
                    openSet.add(successor);
                }
            }
        }

        return null; // Pas de solution
    }

    /**
     * Classe pour stocker la solution
     */
    static class Solution {
        State finalState;
        int nodesExplored;
        long timeMs;

        public Solution(State finalState, int nodesExplored, long timeMs) {
            this.finalState = finalState;
            this.nodesExplored = nodesExplored;
            this.timeMs = timeMs;
        }

        public List<Character> getPath() {
            List<Character> path = new ArrayList<>();
            State current = finalState;
            while (current.parent != null) {
                path.add(0, current.move);
                current = current.parent;
            }
            return path;
        }
    }

    /**
     * Affiche la grille
     */
    private static void printGrid(char[][] grid) {
        for (char[] row : grid) {
            for (char c : row) {
                System.out.print(c);
            }
            System.out.println();
        }
        System.out.println();
    }

    /**
     * Programme principal - Test des deux grilles
     */
    public static void main(String[] args) {
        // Grille 1
        char[][] grid1 = {
                {'■','■','■','■','■','■','■','■','■','■'},
                {'■','□','□','□','□','□','□','□','□','■'},
                {'■','□','■','■','□','■','■','□','□','■'},
                {'■','□','$','□','T','□','$','□','□','■'},
                {'■','□','■','□','@','□','■','□','□','■'},
                {'■','□','$','□','T','□','$','□','□','■'},
                {'■','□','■','■','□','■','■','□','□','■'},
                {'■','□','□','T','□','□','T','□','□','■'},
                {'■','□','□','□','□','□','□','□','□','■'},
                {'■','■','■','■','■','■','■','■','■','■'}
        };

        // Grille 2
        char[][] grid2 = {
                {'■','■','■','■','■','■','■','■','■','■'},
                {'■','T','□','■','□','□','■','□','T','■'},
                {'■','□','■','$','□','□','$','■','□','■'},
                {'■','□','■','□','□','□','□','■','□','■'},
                {'■','□','□','□','@','□','□','□','□','■'},
                {'■','□','■','□','□','□','□','■','□','■'},
                {'■','□','■','$','□','□','$','■','□','■'},
                {'■','T','□','■','□','□','■','□','T','■'},
                {'■','□','□','□','□','□','□','□','□','■'},
                {'■','■','■','■','■','■','■','■','■','■'}
        };

        System.out.println("=== RÉSOLUTION GRILLE 1 ===");
        printGrid(grid1);
        Solution sol1 = solve(grid1);

        if (sol1 != null) {
            System.out.println("✓ Solution trouvée !");
            System.out.println("Nombre de mouvements: " + sol1.getPath().size());
            System.out.println("Nœuds explorés: " + sol1.nodesExplored);
            System.out.println("Temps d'exécution: " + sol1.timeMs + " ms");
            System.out.println("Chemin: " + sol1.getPath());
        } else {
            System.out.println("✗ Aucune solution trouvée");
        }

        System.out.println("\n=== RÉSOLUTION GRILLE 2 ===");
        printGrid(grid2);
        Solution sol2 = solve(grid2);

        if (sol2 != null) {
            System.out.println("✓ Solution trouvée !");
            System.out.println("Nombre de mouvements: " + sol2.getPath().size());
            System.out.println("Nœuds explorés: " + sol2.nodesExplored);
            System.out.println("Temps d'exécution: " + sol2.timeMs + " ms");
            System.out.println("Chemin: " + sol2.getPath());
        } else {
            System.out.println("✗ Aucune solution trouvée");
        }
    }
}