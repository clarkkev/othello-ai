# Othello AI
This project consists of a GUI and strong AI for playing [Othello](https://en.wikipedia.org/wiki/Othello). I built it for the University of Washington's AI course, where it won the class-wide [tournament](https://courses.cs.washington.edu/courses/cse473/11sp/othello/tournament.html). The AI features:
* Bitboard game state representations for fast computation.
* Negamax and NegaScout search with alpha beta pruning.
* Iterative deepening with move ordering. Saved evaluations are used for ordering moves at low ply, various heuristics are used for higher ply. 
* Transposition tables using Zobrist hashing.
* A machine-learning-tuned static evaluation function with a special evaluator for endgames.
* A GUI for playing the game and displaying the AI's evaluations (see below). 

![alt tag](https://github.com/clarkkev/othello-ai/blob/master/screenshots/screenshot.png)
