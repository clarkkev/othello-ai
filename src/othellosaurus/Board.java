package othellosaurus;

import java.util.Arrays;

/**
 * Representation for game state in Othello
 */
public class Board implements Comparable<Board> {
	public static final int WHITE = 0;
	public static final int BLACK = 1;
	public static final int PASS = -1;
	
	// the starting configuration, used mainly for testing purposes
	// 1 for white piece, 2 for black, last index tells which player moves first
	public static final int[] START = new int[]
	    {0, 0, 0, 0, 0, 0, 0, 0, 
		 0, 0, 0, 0, 0, 0, 0, 0, 
		 0, 0, 0, 0, 0, 0, 0, 0, 
		 0, 0, 0, 1, 2, 0, 0, 0, 
		 0, 0, 0, 2, 1, 0, 0, 0, 
		 0, 0, 0, 0, 0, 0, 0, 0, 
		 0, 0, 0, 0, 0, 0, 0, 0, 
		 0, 0, 0, 0, 0, 0, 0, 0, 
		 Board.BLACK};
		 
	public int mover; // current player to move
	public int opponent; // the other player 
	public long[] pieces = new long[2]; // bitboards representing the pieces on the
										// board (one for white pieces, one for black)
	
	public boolean gameOver; // whether this game is over
	public int lastMove; // the last move played on this board
	public int numPieces; // number of pieces on this board
	public long legalMoves; // bitboard representing legal squares the current player can move
	
	public int value; // used for ranking boards (with the compareTo method)
	public int zobrist; // Zobrist hash code for this
					    // see http://en.wikipedia.org/wiki/Zobrist_hashing
	
	/**
	 * Creates a Board with the default configuration
	 */
	public Board() {
		this(START);
	}
	
	/**
	 * Creates a new board with the given start configuration
	 */
	public Board(int[] startConfig) {
		// do precomputations
		if(!Utils.precompuationsDone) {
			Utils.precompute();
		}
		
		// translate START to Board position
		for(int x = 0; x < 8; x++) {
			for(int y = 0; y < 8; y++) {
				// 7 - y because the bitboard is indexed so 0 is the lower left square
				int configIndex = Utils.getIndex(x, 7 - y); 
				int boardIndex = Utils.getIndex(x, y);
				// add pieces to the current board
				if(startConfig[configIndex] == 1) { 
					pieces[WHITE] |= (1L << boardIndex);
					numPieces++;
				} else if(startConfig[configIndex] == 2) {
					pieces[BLACK] |= (1L << boardIndex);
					numPieces++;
				}
			}
		}
		mover = START[64];
		opponent = mover ^ 1;
		gameOver = false;
		
		legalMoves = getMoves(mover);
	}
	
	/**
	 * Creates a duplicate of b
	 */
	public Board(Board b) {
		mover = b.mover;
		opponent = b.opponent;
		pieces = Arrays.copyOf(b.pieces, 2);
		gameOver = b.gameOver;
		legalMoves = b.legalMoves;
		lastMove = b.lastMove;
		numPieces = b.numPieces;
	}
	
	/**
	 * Creates new board given last position and current move
	 */
	public Board(Board lastBoard, int move) {
		// copy fields over
		mover = lastBoard.opponent;
		opponent = lastBoard.mover;
		pieces = Arrays.copyOf(lastBoard.pieces, 2);
		numPieces = lastBoard.numPieces;
		lastMove = move;
		
		// copy variables because this is faster than doing repeated array lookups
		long pPieces = pieces[mover];
		long oPieces = pieces[opponent];
		long oNegated = ~pieces[opponent];
		if(move != PASS) {
			numPieces++;
			
			long flips = 0;
			// compute bitboard for flipped pieces
			// try all directions (right, up, upright, upleft) forward and backward
			for(int direction = 0; direction < 4; direction++) {
				for(int orientation = 0; orientation < 2; orientation++) {
					// some bitboard magic
					// border is to stop wrapping the pieces on the edge over when we shift
					long border = Utils.shiftable[direction][orientation];
					int shift = Utils.shift[direction];
					long testFlips = 0;
					long loc = (1L << move);
					long tmp = 0;
					while(loc != 0) {
						loc &= border;
						loc = (orientation == 0 ? loc >>> shift : loc << shift);
						tmp = loc;
						loc &= oNegated;
						loc &= pPieces;
						testFlips |= loc;
					}
					
					if((tmp & oPieces) != 0) {
						flips |= testFlips;
					}
				}
			}
			
			// flip pieces
			pieces[opponent] |= flips; 
			pieces[mover] &= ~flips;
			// add in newly placed piece
			pieces[opponent] |= (1L << move);
		}
		
		// generate new moves and check if the game is over
		legalMoves = getMoves(mover);
		if(legalMoves == 0 && move == PASS) {
			gameOver = true;
		}
	}

	/**
	 * Generates a bitboard representing the possible moves for p
	 */
	public long getMoves(int p) {
		int o = p ^ 1;
		long m = 0;
		
		//copy variables because this is faster than doing repeated array lookups
		long pNegated = ~pieces[p];
		long oPieces = pieces[o];
		long oNegated = ~pieces[o];
		// try all directions (right, up, upright, upleft) forward and backward
		for(int direction = 0; direction < 4; direction++) {
			for(int orientation = 0; orientation < 2; orientation++) {
				// more bitboard magic
				long border = Utils.shiftable[direction][orientation];
				int shift = Utils.shift[direction];
				long potentials = pieces[p];
				
				// do initial shift once because must flip at least one piece to have a legal move
				potentials &= border;
				potentials = (orientation == 0 ? potentials >>> shift : potentials << shift);
				potentials &= pNegated;
				potentials &= oPieces;
				
				while(potentials != 0) {
					potentials &= border;
					potentials = (orientation == 0 ? potentials >>> shift : potentials << shift);
					potentials &= pNegated;
					m |= (potentials & oNegated);
					potentials &= oPieces;
				}
			}
		}
		return m;
	}
	
	/**
	 * Returns zobrist hash code for this
	 */
	public int hashCode() {
		// hash value already computed, so return it
		if(zobrist != 0) {
			return zobrist;
		}
		
		long white = pieces[WHITE];
		long black = pieces[BLACK];
		zobrist = (mover << 31);
		// Use lookup table in Utils to compute the hash code one row at at time
		for(int y = 0; y < 8; y++) {
			zobrist ^= Utils.rowHashChange[(int)(white & 255)]
			                              [(int)(black & 255)][y];
			white >>= 8;
			black >>= 8;
		}
		return zobrist;
	}
	
	/**
	 * Used to iterate through the legal moves of this board
	 */
	public int getNextMove() {
		return getNextMove(legalMoves);
	}
	
	/**
	 * Used iterate through the moves coinciding with the given bitboard
	 */
	public int getNextMove(long moves) {
		int moveIndex = Utils.bitScanForward(moves);
		legalMoves &= ~(1L << moveIndex);
		return moveIndex;
	}
	
	/**
	 * Returns true iff it is legal for player to place a piece at (x, y).
	 */
	public boolean moveLegal(int x, int y) {
		long mask = (1L << Utils.getIndex(x, y));
		return ((mask & legalMoves) != 0);
	}
	
	/**
	 * Returns the owner of the piece at (x, y) or -1 if that square is empty.
	 */
	public int pieceAt(int x, int y) {
		long mask = (1L << Utils.getIndex(x, y));
		if((pieces[WHITE] & mask) != 0) {
			return WHITE;
		} else if((pieces[BLACK] & mask) != 0) {
			return BLACK;
		}
		return -1;
	}
	
	/**
	 * Prints out an array that can be used to make a new board.
	 * see the variable START for format
	 */
	public String toString() {
		 String config = "{";
		 for(int y = 7; y >= 0; y--) {
		    for(int x = 0; x < 8; x++) {
		    	if(pieceAt(x, y) == WHITE) {
		    		config += "1, ";
		    	} else if(pieceAt(x, y) == BLACK) {
		    		config += "2, ";
		    	} else {
		    		config += "0, ";
		    	}
		    }
		    config += "\n ";
		 }
		 config += mover == WHITE ? "Board.WHITE};" : "Board.BLACK};";
		 return config;
	}
	
	/**
	 * Compares this to b
	 */
	public int compareTo(Board b) {
		return b.value - value;
	}
}
