package components;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Crt represents the glass screen of the visual display Terminal.
 * Almost no emulation logic here - that all happens in Terminal
 * 
 * All painting to the main screen area happens here.
 * 
 * N.B. This class does not need to be aware of the physical screen (or window) geometry,
 * it simply draws pixels/images on the Canvas.  All scaling and sizing
 * is handled by the invoking object (Dasher).
 * 
 * @author steve
 * v. 0.9   Switch to JavaFX (from Swing)
 *          Move zoom factors out (into DasherJ)
 * v. 0.8   Add setZoom method to support resizing
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
public class Crt extends Canvas {
	
	private static final String DASHER_FONT_BDF = "/resources/D410-a-12.bdf";
	private static final int MIN_VISIBLE = 32, MAX_VISIBLE = 128;
	
	private int charWidth = BDFfont.CHAR_PIXEL_WIDTH; 
	private int charHeight = BDFfont.CHAR_PIXEL_HEIGHT; 
	private BDFfont bdfFont;
	
	private Terminal terminal;
	
	private GraphicsContext g;
	
	public static final Color DFLT_BG_COLOR = Color.BLACK;
	public static final Color DFLT_FG_COLOR = Color.WHITE;
	public static final Color DFLT_DIM_COLOR = Color.DARKGREY;
	
	Color bgColor = DFLT_BG_COLOR;
	Color fgColor = DFLT_FG_COLOR;
	Color dimColor = DFLT_DIM_COLOR;
		
	public Crt( Terminal terminal ) {
		
		this.terminal = terminal;
		
		bdfFont = new BDFfont();

		if (!bdfFont.load( getClass().getResourceAsStream(DASHER_FONT_BDF)) ) {
			System.out.printf( "Crt: Fatal Error - Could not load custom Dasher font.\n" );
			System.exit(1);
		}
		
		g = getGraphicsContext2D();		
	}
	
	@Override
	public boolean isResizable() { return true; }
	
	@Override
	public double prefWidth( double width ) { return getWidth(); }
	
	@Override
	public double prefHeight( double height ) { return getHeight(); }
		
	/***
	 * Paint the Crt
	 * 
	 * This is called VERY often - try to be efficient...
	 * 
	 */
	public void paintCrt() {
    	
 //   	System.out.println( "Debug - paintCrt invoked" );
    	   	  	  	
    	renderCharCells( g );
	
    	// draw the cursor - if on-screen
    	synchronized( terminal ) { // don't want cursor being moved while we are drawing it...
    		if (terminal.cursorX < terminal.visible_cols && terminal.cursorY < terminal.visible_lines) {
    			g.setFill( fgColor );
    			g.fillRect( terminal.cursorX * charWidth, terminal.cursorY* charHeight, charWidth, charHeight );
    			if (terminal.display[terminal.cursorY][terminal.cursorX].charValue != ' ') {
    				g.setFill( bgColor );
    				g.drawImage( bdfFont.charReverseImages[(int) terminal.display[terminal.cursorY][terminal.cursorX].charValue], 
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
    private void renderCharCells( GraphicsContext g ) {
    	
    	byte charVal;
  	
       	for (int y = 0; y < terminal.visible_lines; y++) {
    		for (int x = 0; x < terminal.visible_cols; x++) {
    			
    			// first fill the cell with the background colour and set the right foreground colour
    			if (terminal.display[y][x].reverse) {
    				g.setFill( fgColor );
    				g.fillRect( x * charWidth, y * charHeight, charWidth, charHeight );
    				g.setFill( bgColor );
    				g.setStroke( bgColor );
    			} else {
    				g.setFill( bgColor );
    				g.fillRect( x * charWidth, y * charHeight, charWidth, charHeight );
    				g.setFill( fgColor );
    				g.setStroke( fgColor );
    			}
    			
    			// draw the character but handle blinking
    			if (terminal.blinking_enabled && terminal.blinkState && terminal.display[y][x].blink) {
    				g.setFill( bgColor );
    	    		g.fillRect( x * charWidth, (y + 1) * charHeight, charWidth, charHeight );
    			} else {
    				charVal = terminal.display[y][x].charValue;
    				if (charVal >= MIN_VISIBLE && charVal <= MAX_VISIBLE && bdfFont.charLoaded[charVal]) {
    					if (terminal.display[y][x].reverse) {
    						g.drawImage( bdfFont.charReverseImages[(int) charVal], x * charWidth, y * charHeight );
    					} else if (terminal.display[y][x].dim) {
    						g.drawImage( bdfFont.charDimImages[(int) charVal], x * charWidth, y * charHeight );
    					} else {
    						g.drawImage( bdfFont.charImages[(int) charVal], x * charWidth, y * charHeight );
    					}
    				}
       			}
    			
    			// underscore
    			if (terminal.display[y][x].underscore) {
    				g.setLineWidth( 1.0 );    				
    				g.strokeLine( x * charWidth, (y + 1) * charHeight, (x + 1) * charWidth, (y + 1) * charHeight );
    			}
    		}
    	}
    }

}
