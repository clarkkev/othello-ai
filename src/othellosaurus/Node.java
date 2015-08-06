package othellosaurus;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Represents a Node in a game tree.
 *  Search uses:
 *  - Negamax search with alpha-beta pruning
 *  - Transposition tables
 *  - Iterative deepening with move ordering
 *  - History heuristic
 *  - Killer move heuristic
 *
 *  Optional:
 *   - Negascout search: This makes search faster in most positions but slower
 *     when what is considered the best line changes a frequently at high ply.
 *     Unfortunately, this is exactly when we want search to be deep, so in general
 *     it seems to (very slightly) hurt performance
 */
public class Node {
	// types of stored evaluations in the transposition table
	public static final byte EXACT = 0;
	public static final byte LOWER_BOUND = 1;
	public static final byte UPPER_BOUND = -1;

	// just a big power of two
	public static final int WIN_MULTIPLIER = 4194304;

	// max ply at which to check if computations have gone over the time limit
	public static final int FORCE_STOP_PLY = 6;
	// num plies at which to record moves for visual display
	public static final int RECORD_MOVE_PLY = 4;

	public static int orderPly; // max ply at which to do move-ordering
	public static int transposePly; // max ply at which to check transposition
									// table for repeated position
	public static int hashPly; // max ply at which to enter nodes in
							   // transposition table
	public static int negascoutPly; // ply at which to use negascout algorithm

	public static long stopTime; // when to stop searching
	public static byte searchDepth; // depth at which to use static evaluation
	public static int nodesSearched; // number of nodes visited this search
	public static boolean doneStaticEval; // whether a static evaluation has
										  // been done this search
	public static Evaluator evaluator; // Evaluator for static evaluations
	public static final HashMap<Integer, TableEntry> transpositionTable =
		new HashMap<Integer, TableEntry>(1000000, 0.5f);

	public Board b; // Current board position for this search
	public byte ply; // Current ply for this search
	public int bestMove; // Best move found from b
	public int bestValue; // Score of the best move found from b
	public Node bestChild;  // This node's best child node

	// previously found good moves for history heuristic
	long strongMoves;
	long lastStrongMoves;

	/**
	 * Sets when to do various search algorithms based on the current
	 * search depth.
	 */
	public static void setDecisionPlies(boolean negascout) {
		orderPly = Math.min(searchDepth - 4, 9);
		transposePly = Math.min(searchDepth - 3, 10);
		hashPly = Math.max(orderPly + 1, transposePly);
		negascoutPly = negascout ? orderPly - 1 : -1;
	}

	/** Creates a new Node */
	public Node(Board b, byte ply) {
		this.b = b;
		this.ply = ply;
	}

	/** Search */
	public int negaMax(int alpha, int beta) {
	    // stop searching if gone over time
		if(searchDepth == 0 || (ply <= FORCE_STOP_PLY && System.nanoTime() > stopTime)) {
			searchDepth = 0;
			return 0;
		}

		nodesSearched++;

		// game is over, return score of final position
		if(b.gameOver) {
			return store(WIN_MULTIPLIER * Evaluator.pieces(b), EXACT);
		}

		// forced pass
		if(b.legalMoves == 0) {
			Node child = new Node(new Board(b, Board.PASS), (byte)(ply + 1));
			int childValue = -child.negaMax(-beta, -alpha);
			if(childValue > alpha) {
				if(childValue >= beta) {
					return store(childValue, LOWER_BOUND);
				}
				if(ply <= RECORD_MOVE_PLY) {
					bestMove = Board.PASS;
					bestValue = alpha;
					bestChild = child;
				}
				return store(childValue, EXACT);
			}
			return store(alpha, UPPER_BOUND);
		}

		// if we have seen this position before in the current search,
		// avoid repeated computation by using its stored value
		if(ply <= transposePly && transpositionTable.containsKey(b.hashCode())) {
			TableEntry e = transpositionTable.get(b.hashCode());
			if(e.depth == searchDepth) {
				if(e.type == EXACT) {
					return e.v;
				} else if(e.type == LOWER_BOUND) {
					alpha = Math.max(alpha, e.v);
				} else {
					beta = Math.min(beta, e.v);
				}
			}
			if(alpha >= beta) {
				return alpha;
			}
		}

		// at search depth, return static evaluation function
		if(ply >= searchDepth) {
			doneStaticEval = true;
			return store(evaluator.eval(b), EXACT);
		}

		// use history heuristic 2 plies after move ordering and killer move
		// heuristic after that
		boolean killerMoveHeuristic = true;
		boolean historyHeuristic = (ply == orderPly + 1 || ply == orderPly + 2);
		boolean prepareForHistory = (ply == orderPly - 1 || ply == orderPly);

		// move ordering for better alpha-beta pruning performance
		Board[] children = null;
		int numChildren = 0;
		if(ply <= orderPly) {
			children = new Board[31];
			while(b.legalMoves != 0) {
				Board c = children[numChildren] = new Board(b, b.getNextMove());
				c.value = -evaluator.eval(c);
				if(transpositionTable.containsKey(c.hashCode())) {
					TableEntry e = transpositionTable.get(c.hashCode());
					c.value += 67108864 * e.depth;
					c.value -= 4096 * (e.v + e.type);
				}
				numChildren++;
			}
			Arrays.sort(children, 0, numChildren);

			// record the best couple moves for history heuristic
			if(prepareForHistory) {
				lastStrongMoves = strongMoves;
				strongMoves = 0;
				for(int i = 0; i < numChildren/3; i++) {
					strongMoves |= (1L << children[i].lastMove);
				}
			}
		}

		// expand this node
		byte type = UPPER_BOUND;
		int n = 0;

		while(b.legalMoves != 0 || n < numChildren) {
			Board nextBoard;

			if(numChildren != 0) {
				// move ordering
				nextBoard = children[n++];
			} else if(historyHeuristic) {
				// history heuristic: try out moves that were found to be good previously first
				long moves = (b.legalMoves & lastStrongMoves);
				if(moves == 0) {
				 	historyHeuristic = false;
				 	nextBoard = new Board(b, b.getNextMove());
				} else {
					nextBoard = new Board(b, b.getNextMove(moves));
				}
			} else if(killerMoveHeuristic) {
				// killer move heuristic: try corner moves first
				long moves = (b.legalMoves & Utils.corners);
				if(moves == 0) {
					 killerMoveHeuristic = false;
					 nextBoard = new Board(b, b.getNextMove());
				} else {
					nextBoard = new Board(b, b.getNextMove(moves));
				}
			} else {
				// regular move generation
				nextBoard = new Board(b, b.getNextMove());
			}

			Node child = new Node(nextBoard, (byte)(ply + 1));

			// pass on the best couple moves for history heuristic
			if(prepareForHistory) {
				child.strongMoves = strongMoves;
				child.lastStrongMoves = lastStrongMoves;
			} else if(ply == orderPly + 1) {
				child.strongMoves = lastStrongMoves;
				child.lastStrongMoves = strongMoves;
			}

			// NegaScout search
			int childValue;
			if(ply <= negascoutPly && n > 1) {
				long l = nextBoard.legalMoves;
				childValue = -child.negaMax(-alpha - 1, -alpha);
				if(childValue > alpha && childValue < beta) {
					nextBoard.legalMoves = l;
					childValue = -child.negaMax(-beta, -childValue);
				}
			} else {
				childValue = -child.negaMax(-beta, -alpha);
			}

			// new best move found!
			if(childValue > alpha) {
				type = EXACT;
				alpha = childValue;
				if(ply <= RECORD_MOVE_PLY) {
					bestMove = nextBoard.lastMove;
					bestValue = alpha;
					bestChild = child;
				}
			}

			// alpha-beta pruning
			if(alpha >= beta) {
				return store(alpha, LOWER_BOUND);
			}
		}

		return store(alpha, type);
	}

	/** Stores the given value and entry type in the transposition table */
	public int store(int v, byte type) {
		if(ply <= hashPly) {
			transpositionTable.put(b.hashCode(), new TableEntry(v, type));
		}
		return v;
	}
}