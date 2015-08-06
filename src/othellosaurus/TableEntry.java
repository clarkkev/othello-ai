package othellosaurus;

/**
 * Entry for transposition table
 */
public class TableEntry {
	public byte type; // type of entry (see final variables in Node)
	public int v; // value of this entry
	public byte depth; // depth of search which gave this entry's value

	/** Creates new TableEntry */
	public TableEntry(int v, byte type) {
		this.v = v;
		this.type = type;
		this.depth = Node.searchDepth;
	}
}