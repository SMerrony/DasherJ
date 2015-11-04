package components;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import javax.swing.JComponent;

/**
 * Crt represents the glass screen of the visual display Terminal.
 * Almost no emulation logic here - that all happens in Terminal
 * 
 * All painting to the main screen area happens here.
 * 
 * @author steve
 * v. 0.7   Restore default scaling appearance (all chars double-height)
 * 			Small tidy-ups
 * v. 0.6   Rename screen to terminal
 * 			Big performance increase by drawing character BufferedImages rather than individual pixels
 * 			Fix scaling of printing (reduce from 4x to 2x)
 * 			Remove drawChar method and in-line
 * v. 0.5 - Remove 'rasterised' font look
 * 			Improve performance and realism by eliminating affinetransform
 * 			Fix printing to scale and position reasonably
 *
 */
public class Crt extends JComponent implements Printable {
	
	private static final String DASHER_FONT_BDF = "/resources/D410-a-12.bdf";
	private static final int MIN_VISIBLE = 32, MAX_VISIBLE = 128;
	private static final int PTS_PER_INCH = 72;
	private static final float PRINT_SCALE_FACTOR = 2.0f;
	
	private int charWidth = BDFfont.CHAR_PIXEL_WIDTH; 
	private int charHeight = BDFfont.CHAR_PIXEL_HEIGHT; 
	private BDFfont bdfFont;
	
	private Terminal terminal;
	
	private AffineTransform scaleTransform;
	
	Color bgColor = Color.BLACK;
	Color fgColor = Color.WHITE;
	Color dimColor = Color.LIGHT_GRAY;
		
	public Crt( Terminal terminal ) {
//		super();
		
    	scaleTransform = new AffineTransform();
    	scaleTransform.scale( 1.0, 2.0 );
		
		this.terminal = terminal;
		this.setOpaque( true );
		this.setBackground( Color.BLACK );
		this.setForeground( Color.WHITE );
		
		bdfFont = new BDFfont();

		if (!bdfFont.load( getClass().getResourceAsStream(DASHER_FONT_BDF)) ) {
			System.out.printf( "Crt: Fatal Error - Could not load custom Dasher font.\n" );
			System.exit(1);
		}
				
		this.setFocusable( true );
	}

	/** setCharSize set the size of the CRT widget in terms of character cells/
	 * Used by DasherJ.
	 * 
	 * @param y
	 * @param x
	 */
	public void setCharSize( int y, int x ) {

		setPreferredSize( new Dimension( x * charWidth, (y+1) * charHeight * 2) );
	}
	
	
	/***
	 * Paint the Crt
	 * 
	 * This is called VERY often - try to be efficient...
	 * 
	 */
    @Override
	public void paintComponent( final Graphics pG ) {
    	
    	//super.paintComponent( pG );
    	
    	Graphics2D g = (Graphics2D) pG;

    	g.setTransform( scaleTransform );

    	// super.paintComponent( g );

    	renderCharCells( g );
	
    	// draw the cursor - if on-screen
    	synchronized( terminal ) { // don't want cursor being moved while we are drawing it...
    		if (terminal.cursorX < terminal.visible_cols && terminal.cursorY < terminal.visible_lines) {
    			g.setColor( fgColor );
    			g.fillRect( terminal.cursorX * charWidth, terminal.cursorY* charHeight, charWidth, charHeight );
    			if (terminal.display[terminal.cursorY][terminal.cursorX].charValue != ' ') {
    				g.setColor( bgColor );
    				g.drawImage( bdfFont.charReverseImages[(int) terminal.display[terminal.cursorY][terminal.cursorX].charValue], 
    							 null, 
    							 terminal.cursorX * charWidth, 
    							 terminal.cursorY * charHeight );
    			}
    		}
    	}
    	

    }

    /***
     * Paint individual character cells onto the passed in Graphics object (usually the Crt - but may be
     * a separate image for printing etc...) 
     * 
     * Called often - definitely don't waste time in here!
     * 
     * @param g
     */
    private void renderCharCells( Graphics2D g ) {
    	
    	byte charVal;
  	
       	for (int y = 0; y < terminal.visible_lines; y++) {
    		for (int x = 0; x < terminal.visible_cols; x++) {
    			
    			// first fill the cell with the background colour and set the right foreground colour
    			if (terminal.display[y][x].reverse) {
    				g.setColor( fgColor );
    				g.fillRect( x * charWidth, y * charHeight, charWidth, charHeight );
    				g.setColor( bgColor );
    			} else {
    				g.setColor( bgColor );
    				g.fillRect( x * charWidth, y * charHeight, charWidth, charHeight );
    				g.setColor( fgColor );
    			}
    			
    			// draw the character but handle blinking
    			if (terminal.blinking_enabled && terminal.blinkState && terminal.display[y][x].blink) {
    				g.setColor( bgColor );
    	    		g.fillRect( x * charWidth, (y + 1) * charHeight, charWidth, charHeight );
    			} else {
    				charVal = terminal.display[y][x].charValue;
    				if (charVal >= MIN_VISIBLE && charVal <= MAX_VISIBLE && bdfFont.charLoaded[charVal]) {
    					if (terminal.display[y][x].reverse) {
    						g.drawImage( bdfFont.charReverseImages[(int) charVal], null, x * charWidth, y * charHeight );
    					} else if (terminal.display[y][x].dim) {
    						g.drawImage( bdfFont.charDimImages[(int) charVal], null, x * charWidth, y * charHeight );
    					} else {
    						g.drawImage( bdfFont.charImages[(int) charVal], null, x * charWidth, y * charHeight );
    					}
    				}
       			}
    			
    			// underscore
    			if (terminal.display[y][x].underscore) {
    				g.drawLine( x * charWidth, (y + 1) * charHeight, (x + 1) * charWidth, (y + 1) * charHeight );
    			}
    		}
    	}
    }
    
	/* We don't actually print the screen graphic here, a new graphic is drawn for printing
     * 
     * @see java.awt.print.Printable#print(java.awt.Graphics, java.awt.print.PageFormat, int)
     */
    @Override
    public int print( final Graphics pG, final PageFormat pageFormat, final int pageIndex ) {
    	
    	if (pageIndex == 0) {
    		
    		Graphics2D g = (Graphics2D) pG;
    		
    		// set origin quarter-of-an-inch from page edges
    		g.translate( (int) (pageFormat.getImageableX() + PTS_PER_INCH/4), (int) (pageFormat.getImageableY() + PTS_PER_INCH/4) );
			AffineTransform printTransform = new AffineTransform( pageFormat.getMatrix() );
    		printTransform.scale(  PRINT_SCALE_FACTOR, PRINT_SCALE_FACTOR );
    		printTransform.translate( 200, 200);
    		g.setTransform( printTransform );
    		
    		// save the current color scheme
    		Color savedBgColor = bgColor, savedDimColor = dimColor, savedFgColor = fgColor;
    		
    		// set sensible colors for printing
    		bgColor = Color.WHITE;
    		dimColor = Color.GRAY;
    		fgColor = Color.BLACK;
    		    		
    		renderCharCells( g );
    		
    		// restore colors
    		bgColor = savedBgColor;
    		dimColor = savedDimColor;
    		fgColor = savedFgColor;
    		
    		return PAGE_EXISTS;
    	} else {
    		return NO_SUCH_PAGE;
    	}
    }

}
