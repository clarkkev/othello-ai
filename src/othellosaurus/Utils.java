package othellosaurus;

/**
 * Class for bitboard utilities
 * Also stores look up tables for fast computation
 */
public class Utils {
	// Whether pre-computations are done
	public static boolean precompuationsDone = false;

	// The amount of bitshifting necessary to move one square in the
	// given direction (up, right, up-right, up-left)
	public static final int[] shift = {1, 8, 9, 7};
	// gives the squares that can be shifted in the given direction and
	// orientation while staying on the board
	public static final long[][] shiftable =
		{{0xfefefefefefefefeL, 0x7f7f7f7f7f7f7f7fL},
		 {0xffffffffffffff00L, 0x00ffffffffffffffL},
		 {0xfefefefefefefe00L, 0x007f7f7f7f7f7f7fL},
		 {0x7f7f7f7f7f7f7f00L, 0x00fefefefefefefeL}};
	public static final long[][] edges =
		{{0x0101010101010101L, 0x8080808080808080L},
		 {0x00000000000000ffL, 0xff00000000000000L},
		 {0x01010101010101ffL, 0xff80808080808080L},
		 {0x80808080808080ffL, 0xff01010101010101L}};
	public static long[] frontierContributers = new long[4];
	// some other useful bitboards
	public static long corners = 0x8100000000000081L;
	public static long center = 0x00003c3c3c3c0000L;

	// stores the number of bits in the given 16-bit number
	public static final int[] bitCount = new int[65536];
	// stores a randomly generated bit string to be XORed with a board's hash
	// code for a piece at the given index and of the given color
	public static final int[][] hashChange = new int[64][2];
	// stores the bit string to be XORed with the board's hash code given the
	// white pieces, black pieces, and number of the given row
	public static final int[][][] rowHashChange = new int[256][256][8];
	// generalized the 4x4 SQUARE_VALUES in Evaluator to the whole board
	public static final int[][] fullSquareScore = new int[8][8];
	// stores the piece placement value for the given set of
	// white pieces, black pieces, and number of the given row
	public static final int[][][] rowScore = new int[256][256][8];
	// stores mobility score for given number of white and black moves;
	public static final int[][] mobilityScore = new int[64][64];

	/** Precomputes values for later look up */
	public static void precompute() {
		for(int i = 0; i < 4; i++) {
			frontierContributers[i] = ~(edges[i][0] | edges[i][1]);
		}

		// bitcount setup
		for(int i = 0; i < 65536; i++) {
			int n = i;
			int count = 0;
			while (n != 0) {
				count++;
				n &= (n - 1);
			}
			bitCount[i] = count;
		}

		// hash change setup
		for(int i = 0; i < 64; i++) {
			for(int j = 0; j < 2; j++) {
				for(int k = 0; k < 31; k++) {
					if(Math.random() < 0.5) {
						hashChange[i][j] |= (1 << k);
					}
				}
			}
		}

		// piece placement value setup
		for(int x = 0; x < 4; x++) {
			for(int y = 0; y < 4; y++) {
				fullSquareScore[x][y] = Evaluator.SQUARE_SCORE[x][y];
				fullSquareScore[7 - x][y] = Evaluator.SQUARE_SCORE[x][y];
				fullSquareScore[x][7 - y] = Evaluator.SQUARE_SCORE[x][y];
				fullSquareScore[7- x][7 - y] = Evaluator.SQUARE_SCORE[x][y];
			}
		}

		// precompute hashChange and score for an arbitrary row of pieces
		// iterate through all possible configurations of white and black pieces
		for(int white = 0; white < 256; white++) {
			for(int black = 0; black < 256; black++) {
				if((white & black) != 0) {
					continue;
				}
				// iterate through all rows
				for(int y = 0; y < 8; y++) {
					int score = 0;
					// iterate through all squares in the current row
					for(int x = 0; x < 8; x++) {
						int index = getIndex(x, y);
						if((white & (1 << x)) != 0) {
							rowHashChange[white][black][y] ^= hashChange[index][Board.WHITE];
							score += fullSquareScore[x][y];
						} else if((black & (1 << x)) != 0) {
							rowHashChange[white][black][y] ^= hashChange[index][Board.BLACK];
							score -= fullSquareScore[x][y];
						}
					}
					rowScore[white][black][y] = score;
				}
			}
		}

		// precompute mobility scores
		for(int i = 0; i < 64; i++) {
			for(int j = 0; j < 64; j++) {
				mobilityScore[i][j] =
					(int)Math.sqrt(Evaluator.MOBILITY_FACTOR * i) -
					(int)Math.sqrt(Evaluator.MOBILITY_FACTOR * j);
			}
		}

		precompuationsDone = true;
	}

	/** Prints the given bitboard (for debugging purposes) */
	public static void printBitboard(long BB) {
	    for(int y = 7; y >= 0; y--) {
	    	for(int x = 0; x <= 7; x++) {
	    		System.out.print((1 & (BB >> getIndex(x, y))) + " ");
	    	}
	    	System.out.println();
	    }
	    System.out.println();
	}

	/** Returns the index (0 - 63) corresponding to the square at (x, y) */
	public static int getIndex(int x, int y) {
		return x + 8*y;
	}

	/**
	 * Returns the notation for the square at index.
	 * Columns are labeled a-h from the left column to the right one
	 * Rows are labeled 1-8 from the top to the bottom.
	 */
	public static String getMoveNotation(int move) {
		return move == -1 ? "pass" : (char)('a' + move % 8)  + "" + (8 - move / 8);
	}

	/** Returns the number of ones in the given bit string */
	public static int bitCount(long b) {
		return bitCount[(int)(b & 65535)]
		     + bitCount[(int)((b >> 16) & 65535)]
		     + bitCount[(int)((b >> 32) & 65535)]
		     + bitCount[(int)((b >> 48) & 65535)];
	}

	/**
	 * Fast bitscan method I found online. Returns the index of the first bit
	 * in the given bitString
	 *
     * @author Matt Taylor
     * @return index 0..63
     * @param bb a 64-bit word to bitscan, should not be zero
     */
	private static final int[] foldedTable = {
    	63,30, 3,32,59,14,11,33,
    	60,24,50, 9,55,19,21,34,
    	61,29, 2,53,51,23,41,18,
    	56,28, 1,43,46,27, 0,35,
    	62,31,58, 4, 5,49,54, 6,
    	15,52,12,40, 7,42,45,16,
    	25,57,48,13,10,39, 8,44,
    	20,47,38,22,17,37,36,26,
    };
	public static int bitScanForward(long b) {
    	b ^= (b - 1);
        int folded = ((int)b) ^ ((int)(b >>> 32));
        return foldedTable[(folded * 0x78291ACF) >>> 26];
    }
}
