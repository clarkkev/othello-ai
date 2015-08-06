package othellosaurus;

/**
 * Othello static evaluator
 */
public class Evaluator {
	public static final int MOBILITY_FACTOR = 10000;

	// weights for heuristics given number of pieces on the board
	public int[][] weightsForNumPieces;

	/**
	 * Constructs a new evaluator
	 * weightsForTimings is an array of weights. Timings is an array that
	 * determines at which number of pieces on the board to use the matching
	 * set of weights.
	 * When the current number of pieces is not set in timings, weights are
	 * linearly interpolated.
	 */
	public Evaluator(int[][] weightsForTimings, int[] timings) {
		weightsForNumPieces = new int[65][weightsForTimings[0].length];

		for(int m = 0; m <= 64; m++) {
			// determine which set of weights to use
			int w = 0;
			for(int i = 0; i < timings.length; i++) {
				if(m <= timings[i]) {
					w = i;
					break;
				}
			}

			// first set of weights: just return them
			if(w == 0) {
				weightsForNumPieces[m] = weightsForTimings[0];
				continue;
			}

			// linearly interpolate between the set of weights given for the
			// current number of moves and the previous set of weights
			double factor = ((double)m - timings[w - 1]) / (timings[w] - timings[w - 1]);
			for(int i = 0; i < weightsForTimings[w].length; i++) {
				weightsForNumPieces[m][i] =
					(int)Math.rint(factor * weightsForTimings[w][i]
			                    + (1 - factor) * weightsForTimings[w - 1][i]);
			}
		}
	}

	/**
	 * Returns a static evaluation for b
	 */
	public int eval(Board b) {
		int score = 0;
		int[] weights = weightsForNumPieces[b.numPieces];

		if(weights[0] != 0) {
			score += weights[0] * mobility(b);
		}
		if(weights[1] != 0) {
			score += weights[1] * frontier(b);
		}
		if(weights[2] != 0) {
			score += weights[2] * pieces(b);
		}
		if(weights[3] != 0) {
			score += weights[3] * placement(b);
		}
		if(weights[4] != 0) {
			score += weights[4] * stability(b);
		}
		if(weights[5] != 0) {
			score += weights[5] * cornerGrab(b);
		}

		return score;
	}

	/**
	 * Returns the number of legal moves available to the player about to move
	 * minus the number of legal moves available to the other player
	 */
	public static int mobility(Board b) {
		long opponentMoves = b.getMoves(b.opponent);
		return Utils.mobilityScore[Utils.bitCount(b.legalMoves)][Utils.bitCount(opponentMoves)];
	}

	/**
	 * Returns the number of spaces adjacent to opponent pieces minus the
	 * the number of spaces adjacent to the current player's pieces.
	 */
	public static final int frontier(Board b) {
		long moverPieces = b.pieces[b.mover];
		long opponentPieces = b.pieces[b.opponent];
		long spaces = ~(moverPieces | opponentPieces);

		long pfront = 0;
		long ofront = 0;
		// check for empty spaces in each direction
		for(int direction = 0; direction < 4; direction++) {
			int shift = Utils.shift[direction];
			long mask = Utils.frontierContributers[direction];
			pfront |= (spaces & ((moverPieces & mask)
					>>> shift));
			pfront |= (spaces & ((moverPieces & mask)
					<< shift));
			ofront |= (spaces & ((opponentPieces & mask)
					>>> shift));
			ofront |= (spaces & ((opponentPieces & mask)
					<< shift));
		}

		return Utils.bitCount(ofront) - Utils.bitCount(pfront);

	}

	/**
	 * Returns the number of pieces owned by the player about to move minus
	 * the number of pieces owned by the other player
	 */
	public static int pieces(Board b) {
		return Utils.bitCount(b.pieces[b.mover])
			 - Utils.bitCount(b.pieces[b.opponent]);
	}

	/**
	 * Returns the number of stable disks owned by the player about to move
	 * minus the number of stable disks owned by the other player
	 */
	public static int stability(Board b) {
		return stableDisks(b, b.mover) - stableDisks(b, b.mover ^ 1);
	}

	/**
	 * Returns the number of stable pieces owned by the player about to move minus
	 * the number of stable pieces owned by the other player
	 */
	public static int stableDisks(Board b, int p) {
		long pPieces = b.pieces[p];
		long stable = Utils.corners & pPieces;
		long newStable = 0;

		while(stable != newStable) {
			stable = newStable;
			newStable = pPieces;
			for(int dir = 0; dir < 4; dir++) {
				newStable &= (Utils.edges[dir][0] | Utils.edges[dir][1]
			        | (stable << Utils.shift[dir]) | (stable >>> Utils.shift[dir]));
			}
		}

		return Utils.bitCount(stable);
	}

	// value of controlling the given square
	public static final int[][] SQUARE_SCORE =
	{{ 100, -10,   8,   6},
	 { -10, -25,  -4,  -4},
	 {   8,  -4,   6,   4},
	 {   6,  -4,   4,   0}};
	/**
	 * Returns the piece placement score of the current player minus the piece
	 * placement score of the opponent. See SQUARE_SCORE for values.
	 */
	public static int placement(Board b) {
		long playerPieces = b.pieces[b.mover];
		long opponentPieces = b.pieces[b.opponent];
		int score = 0;

		// Use lookup table in Utils to compute placement value one row at at time
		for(int y = 0; y < 8; y++) {
			score += Utils.rowScore[(int)(playerPieces & 255)]
			                       [(int)(opponentPieces & 255)][y];

			playerPieces >>= 8;
			opponentPieces >>= 8;
		}

		return score;
	}

	/**
	 * Returns 1 if the current player can take a corner with its next move
	 * and 0 if otherwise.
	 */
	public static int cornerGrab(Board b) {
		return (b.legalMoves & Utils.corners) == 0 ? 0 : 1;
	}

	/**
	 * Returns 1 if the current player has parity (that is, they will move last)
	 * and -1 if otherwise (currently unused feature)
	 */
	public static int parity(Board b) {
		return b.opponent == (b.numPieces + b.mover) % 2 ? 1 : -1;
	}
}
