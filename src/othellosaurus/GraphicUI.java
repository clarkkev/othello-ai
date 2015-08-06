package othellosaurus;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Hashtable;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * GUI for Othello-playing program
 */
@SuppressWarnings("serial")
public class GraphicUI extends JFrame implements ActionListener, ChangeListener {
	public static final int SQUARE_LENGTH = 60; // size for squares to be displayed
	public static final int X_BUFFER = 23; // the space to leave around xs denoting
										   // legal squares to move in
	public static final int PIECE_BUFFER = 4; // space to leave around drawing of a piece

	private Board gameBoard; // currently displayed position
	private final Stack<Board> gameHistory; // positions that have occurred so far

	private final Square[][] squares = new Square[8][8]; // squares for displaying board
	private final JLabel textDisplay; // text display for the board's score
	private final JLabel computerOutput;
	private final JSlider timeSlider;

	public boolean locked = false;
	private String outputText;

	// Agents controlling white and black
	// Weights for end game found through linear regression on the final score
	// Other weights found through hill climbing algorithm
	private final Agent whiteBot =
		new Agent(new Evaluator(new int[][] {
				{8, 85, -40, 10, 210, 520},
			    {8, 85, -40, 10, 210, 520},
			    {33, -50, -15, 4, 416, 2153},
			    {46, -50, -1, 3, 612, 4141},
			    {51, -50, 62, 3, 595, 3184},
			    {33, -5,  66, 2, 384, 2777},
			    {44, 50, 163, 0, 443, 2568},
			    {13, 50, 66, 0, 121, 986},
			    {4, 50, 31, 0, 27, 192},
			    {8, 500, 77, 0, 36, 299}},
			new int[] {0, 55, 56, 57, 58, 59, 60, 61, 62, 63}),
			false, 100, 1);
	private final Agent blackBot =
		new Agent(new Evaluator(new int[][] {
				{8, 85, -40, 10, 210, 520},
			    {8, 85, -40, 10, 210, 520},
			    {33, -50, -15, 4, 416, 2153},
			    {46, -50, -1, 3, 612, 4141},
			    {51, -50, 62, 3, 595, 3184},
			    {33, -5,  66, 2, 384, 2777},
			    {44, 50, 163, 0, 443, 2568},
			    {13, 50, 66, 0, 121, 986},
			    {4, 50, 31, 0, 27, 192},
			    {8, 500, 77, 0, 36, 299}},
			new int[] {0, 55, 56, 57, 58, 59, 60, 61, 62, 63}),
			false, 100, 1);

	/** Runs the GUI */
	public static void main(String[] args) {
		GraphicUI gui = new GraphicUI();
		gui.setVisible(true);
	}

	/** Creates a new GUI */
	public GraphicUI() {
		setTitle("Othello");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel main = new JPanel();
		main.setLayout(new BorderLayout());

		JPanel board = new JPanel();
		board.setLayout(new GridLayout(8, 8));
		for(int y = 7; y >= 0; y--) {
			for(int x = 0; x < 8; x++) {
				board.add(squares[x][y] = new Square(x, y));
			}
		}
		board.setMaximumSize(new java.awt.Dimension(480, 480));
		board.setMinimumSize(new java.awt.Dimension(480, 480));
		board.setPreferredSize(new java.awt.Dimension(480, 480));

		JPanel sidePanel = new JPanel();
		sidePanel.setLayout(new BorderLayout());

		computerOutput = new JLabel(" ", SwingConstants.LEFT);
		computerOutput.setVerticalAlignment(SwingConstants.TOP);
		computerOutput.setMaximumSize(new java.awt.Dimension(220, 440));
        computerOutput.setMinimumSize(new java.awt.Dimension(220, 480));
        computerOutput.setPreferredSize(new java.awt.Dimension(220, 440));
        computerOutput.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        textDisplay = new JLabel(" ");
        textDisplay.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        sidePanel.add(textDisplay, BorderLayout.NORTH);
		sidePanel.add(computerOutput, BorderLayout.SOUTH);


		main.add(board, BorderLayout.WEST);
		main.add(sidePanel, BorderLayout.EAST);

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		JButton newGameButton = new JButton("New Game");
		newGameButton.addActionListener(this);
		buttonPanel.add(newGameButton);
		JButton takebackButton = new JButton("Take Back Move");
		takebackButton.addActionListener(this);
		buttonPanel.add(takebackButton);
		JButton computerMoveButton = new JButton("Computer Move");
		computerMoveButton.addActionListener(this);
		buttonPanel.add(computerMoveButton);

		timeSlider = new JSlider(JSlider.HORIZONTAL, 0, 500, 100);
		timeSlider.setMajorTickSpacing(100);
		timeSlider.setPaintTicks(true);
		JPanel sliderPanel = new JPanel();
		sliderPanel.setLayout(new BorderLayout());
		JLabel timeLabel = new JLabel("Computer thought time (sec)");
		Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
		labelTable.put(0, new JLabel("0.1"));
		for (int i = 0; i < 5; i++) {
			labelTable.put((i + 1) * 100, new JLabel(Integer.toString(i + 1)) );
		}
		timeSlider.setLabelTable( labelTable );
		timeSlider.setPaintLabels(true);
		timeSlider.addChangeListener(this);
		sliderPanel.add(timeLabel, BorderLayout.NORTH);
		sliderPanel.add(timeSlider, BorderLayout.SOUTH);

		buttonPanel.add(sliderPanel);

		setLayout(new BorderLayout());
		add(main, BorderLayout.NORTH);
		add(buttonPanel, BorderLayout.SOUTH);

		pack();
		setSize(700, 600);
		setResizable(false);

		gameBoard = new Board();
		gameHistory = new Stack<Board>();
		setText();
		paintSquares();
	}

	/** Plays the move at (x, y) if it is legal */
	public void tryMove(int x, int y) {
		if(gameBoard.moveLegal(x, y)) {
			gameHistory.push(gameBoard);

			gameBoard = new Board(gameBoard, Utils.getIndex(x, y));
			if(gameBoard.legalMoves == 0) { // forced pass
				gameBoard = new Board(gameBoard, Board.PASS);
			}
			setText();
			paintSquares();
		}
	}

	/** Prints white's static evaluation for the board and the current features */
	public void printEval() {
		int m = gameBoard.mover == Board.WHITE ? 1 : -1;
		System.out.println("SCORE: " + m * whiteBot.e.eval(gameBoard));
		System.out.println("MOBILITY: " + m * Evaluator.mobility(gameBoard));
		System.out.println("FRONTIER: " + m * Evaluator.frontier(gameBoard));
		System.out.println("PLACEMENT: " + m * Evaluator.placement(gameBoard));
		System.out.println("STABILITY: " + m * Evaluator.stability(gameBoard));
		System.out.println("CORNER GRAB: " + m * Evaluator.cornerGrab(gameBoard));
		System.out.println();
	}

	public void clearComputerOutput() {
		outputText = "";
		computerOutput.setText("");
	}

	public void extendOutput(String s) {
		if(!outputText.equals("")) {
			outputText += "<BR>";
		}
		outputText += s;
		computerOutput.setText("<HTML>" + outputText + "</HTML>");
		paint(getGraphics());
	}

	/** Sets the text on the textDisplay to the board's score */
	public void setText() {
		textDisplay.setText((gameBoard.gameOver ? "<HTML> Game Over!  <BR>" : "<HTML>")
			+ "White Score = " + Utils.bitCount(gameBoard.pieces[Board.WHITE])
			+ "<BR> Black Score = " + Utils.bitCount(gameBoard.pieces[Board.BLACK])
		  + (gameBoard.gameOver ? "" : "<BR> "
			+ (gameBoard.mover == Board.WHITE ? "White" : "Black") + " to move</HTML>"));
		paintSquares();
	}

	/** Draws the current board position */
	public void paintSquares() {
		super.repaint();
		for(int x = 0; x < 8; x++) {
			for(int y = 0; y < 8; y++) {
				squares[x][y].repaint();
			}
		}
		super.repaint();
	}

	public void paint(Graphics g) {
		super.paint(g);
		for(int x = 0; x < 8; x++) {
			for(int y = 0; y < 8; y++) {
				squares[x][y].paint(g);
			}
		};
		super.paint(g);
	}

	public void stateChanged(ChangeEvent e) {
		if(!timeSlider.getValueIsAdjusting()) {
			int v = Math.max(timeSlider.getValue(), 10);
			whiteBot.maxTime = (long)(v * 1e9 / 100);
			blackBot.maxTime = (long)(v * 1e9 / 100);
		}
	}

	/** User input */
	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("New Game")) {
			// restart game
			gameHistory.clear();
			gameBoard = new Board();
			setText();
			paintSquares();
		} else if(e.getActionCommand().equals("Take Back Move")) {
			// take back last move
			if(!gameHistory.isEmpty()) {
				gameBoard = gameHistory.pop();
				setText();
				paintSquares();
			}
		} else if(e.getActionCommand().equals("Computer Move")) {
			// have AI move
			if(!gameBoard.gameOver) {
				if (gameBoard.mover == Board.WHITE) {
				    whiteBot.getMove(new Board(gameBoard), this);
				} else {
					blackBot.getMove(new Board(gameBoard), this);
				}
			}
		} else {
			// print out current board position as an array
			System.out.println(gameBoard);
			System.out.println();
		}
	}


	/**
	 * A square used for visually displaying the current game state.
	 */
	private class Square extends JPanel implements MouseListener {
		private final int x, y;

		/**
		 * Creates a square corresponding to the square at (x, y) on the
		 * Othello board.
		 */
		public Square(int x, int y) {
			this.x = x;
			this.y = y;
			setPreferredSize(new Dimension(SQUARE_LENGTH, SQUARE_LENGTH));
			setBackground(new Color(25, 140, 50));
			setBorder(BorderFactory.createLineBorder(new Color(5, 30, 10)));
			addMouseListener(this);
		}

		/** Draws the piece currently in this square if it is not empty. */
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			int piece = gameBoard.pieceAt(x, y);
			if(piece != -1) {
				g.setColor(piece == Board.WHITE ? Color.white : Color.black);
				g.fillOval(PIECE_BUFFER, PIECE_BUFFER,
					SQUARE_LENGTH - 2*PIECE_BUFFER,
					SQUARE_LENGTH - 2*PIECE_BUFFER);
			} else if(gameBoard.moveLegal(x, y)) {
				g.setColor(new Color(75, 255, 150));
				g.drawLine(X_BUFFER, X_BUFFER,
					SQUARE_LENGTH - X_BUFFER,
					SQUARE_LENGTH - X_BUFFER);
				g.drawLine(X_BUFFER, SQUARE_LENGTH - X_BUFFER,
					SQUARE_LENGTH - X_BUFFER,
					X_BUFFER);
			}
		}

		/** Allows a human player to move by clicking on the square. */
		public void mouseClicked(MouseEvent e) {
			tryMove(x, y);
		}

		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
	}
}