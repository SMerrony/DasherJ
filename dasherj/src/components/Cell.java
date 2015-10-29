package components;

public final class Cell {
	
	public byte charValue;
	public boolean blink, dim, reverse, underscore, protect;
	
	public final void set(  final byte cv, final boolean bl, final boolean dm, final boolean rev, final boolean under, final boolean prot ) {
		charValue = cv;
		blink = bl;
		dim = dm;
		reverse = rev;
		underscore = under;
		protect = prot;
	}
	
	public final void clearToSpace() {
		charValue = ' ';
		blink = false;
		dim = false;
		reverse = false;
		underscore = false;
		protect = false;
	}
	
	public final void clearToSpaceIfUnprotected() {
		if (!protect) {
			clearToSpace();
		}
	}
	
	public final void copy( final Cell fromCell ) {
		this.charValue = fromCell.charValue;
		this.blink = fromCell.blink;
		this.dim = fromCell.dim;
		this.reverse = fromCell.reverse;
		this.underscore = fromCell.underscore;
		this.protect = fromCell.protect;
	}
	
}
