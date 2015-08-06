package othellosaurus;

/**
 * Computer Othello player
 */
public class Agent {
	public final Evaluator e; // determines this agent's static
					          // evaluation function
	public final int maxDepth; // the maximum ply this agent is allowed to search
	public long maxTime; // the maximum time in seconds this agent is
						 // allowed to think for a move.
	private final boolean negaScout; // whether to use the negaScout algorithm

	/** Creates a new agent */
	public Agent(Evaluator e, boolean negaScout,
			int maxDepth, double maxTime) {
		this.e = e;
		this.negaScout = negaScout;
		this.maxDepth = maxDepth;
		this.maxTime = (long)(maxTime * 1e9);
	}

	/** Searches and returns the agent's move */
	public int getMove(Board b, GraphicUI g) {
		g.locked = true;
		g.clearComputerOutput();

		Node n = new Node(b, (byte)0);
		long startTime = System.nanoTime();
		int bestMove = 0;

		// reset node fields
		Node.nodesSearched = 0;
		Node.evaluator = e;
		Node.transpositionTable.clear();

		// iterative deepening search
		for(Node.searchDepth = 1; Node.searchDepth <= maxDepth; Node.searchDepth++) {
			// stop evaluating if we're past our time limit
			Node.stopTime = startTime + maxTime;
			if(System.nanoTime() > Node.stopTime) {
				break;
			}
			Node.doneStaticEval = false;
			Node.setDecisionPlies(negaScout);

			// search
			n.b.legalMoves = n.b.getMoves(n.b.mover);
			n.negaMax(-Node.WIN_MULTIPLIER * 128, Node.WIN_MULTIPLIER * 128);

			if(System.nanoTime() > Node.stopTime) {
				break;
			}

			bestMove = n.bestMove;

			// print <current search depth> (<score of best move>) <optimal line>
			String s = Integer.toString(Node.searchDepth);
			if(Math.abs(n.bestValue) >= Node.WIN_MULTIPLIER) {
			    // game is solved: print winner and final score with optimal play
				s += (" (" + (
					n.bestValue * (n.b.mover == Board.WHITE ? 1 : -1) > 0 ?
					"White wins with score " : "Black wins with score ")
					+ (Math.abs(n.bestValue / Node.WIN_MULTIPLIER)) + ") ");
			} else {
				if(b.numPieces + Node.searchDepth >= 56) {
					// endgame: score printed so a stable disc is worth 1 point
					s += String.format(" (%1.2fe) ", n.bestValue /
									((float)Node.evaluator.weightsForNumPieces[Math.min(63, b.numPieces + Node.searchDepth)][4]));
				} else {
					// rest of the game: score printed so owning a corner is worth 1 point
					s += String.format(" (%1.2f) ", n.bestValue /
								(100.0 * Node.evaluator.weightsForNumPieces[b.numPieces + Node.searchDepth][3]));
				}
			}
			Node m = n;
			while(m.bestChild != null) {
				s += (Utils.getMoveNotation(m.bestMove) + " ");
				m = m.bestChild;
			}
			g.extendOutput(s);

			// last search did no static evaluations so can stop searching
			// (the remainder of the game is solved)
			if(!Node.doneStaticEval) {
				break;
			}
		}

		long endTime = System.nanoTime();
		g.extendOutput("NODES SEARCHED: " + Node.nodesSearched);
		g.extendOutput(String.format("SECONDS IN THOUGHT: %.3f\n",
				((endTime - startTime) / 1e9)));
		g.extendOutput(String.format("NODES PER SECOND: %.0f\n",
				(1e9 * Node.nodesSearched / (endTime - startTime))));

		n.b.legalMoves = n.b.getMoves(n.b.mover);
		int move = bestMove;
		int x = move % 8;
	    int y = move / 8;
	    g.tryMove(x, y);
	    g.locked = false;
	    return bestMove;
	}
}
