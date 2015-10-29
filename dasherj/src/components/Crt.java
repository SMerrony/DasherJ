package components;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import javax.swing.JPanel;

/**
 * Crt represents the glass screen of the visual display Terminal.
 * Almost no emulation logic here - that all happens in Terminal
 * 
 * All painting to the main screen area happens here.
 * 
 * @author steve
 * 
 * v. 0.5 - Remove 'rasterised' font look
 * 			Improve performance and realism by eliminating affinetransform
 * 			Fix printing to scale and position reasonably
 *
 */
public class Crt extends JPanel implements Printable {
	
	private static final String DASHER_FONT_BDF = "/resources/D410-a-12.bdf";	
	private static final int PTS_PER_INCH = 72;
	private static final float PRINT_SCALE_FACTOR = 4.0f;
	
	private int charWidth = BDFfont.CHAR_PIXEL_WIDTH; 
	private int charHeight = BDFfont.CHAR_PIXEL_HEIGHT * 2; 
	private final BasicStroke  smoothStroke;
	private BDFfont bdfFont;
	
	private Terminal screen;
	
	Color bgColor = Color.BLACK;
	Color fgColor = Color.GREEN;
	Color dimColor = Color.decode( "0x008800" ); // half-green
		
	public Crt( Terminal screen ) {
		super();
		
		smoothStroke = new BasicStroke( 1.5f,  BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND );
		
		this.screen = screen;
		this.setOpaque( true );
		this.setBackground( Color.BLACK );
		this.setForeground( Color.GREEN );
		
		bdfFont = new BDFfont();

		if (!bdfFont.load( getClass().getResourceAsStream(DASHER_FONT_BDF)) ) {
			System.out.printf( "Crt: Fatal Error - Could not load custom Dasher font.\n" );
			System.exit(1);
		}
				
		this.setFocusable( true );
	}

	public void setCharSize( int y, int x ) {
		// setPreferredSize( new Dimension( (int) (x * charWidth * xScale), (int) (y * charHeight * yScale) ) );
		setPreferredSize( new Dimension( (int) (x * charWidth), (int) (y * charHeight) ) );
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
    	// g.setTransform( scaleTransform );
    	g.setStroke( smoothStroke );
    	
    	super.paintComponent( g );

    	renderCharCells( g );
    	
    	// draw the cursor - if on-screen
    	synchronized( screen ) { // don't want cursor being moved while we are drawing it...
    		if (screen.cursorX < Terminal.VISIBLE_COLS && screen.cursorY < Terminal.VISIBLE_COLS) {
    			g.setColor( fgColor );
    			g.fillRect( screen.cursorX * charWidth, screen.cursorY* charHeight, charWidth, charHeight );
    			if (screen.display[screen.cursorY][screen.cursorX].charValue != ' ') {
    				g.setColor( bgColor );
    				drawChar( g, (byte) ' ', screen.cursorX * charWidth, (screen.cursorY + 1) * charHeight );
    				drawChar( g, screen.display[screen.cursorY][screen.cursorX].charValue, screen.cursorX * charWidth, (screen.cursorY + 1) * charHeight );
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
    	
       	for (int y = 0; y < Terminal.VISIBLE_ROWS; y++) {
    		for (int x = 0; x < Terminal.VISIBLE_COLS; x++) {
    			
    			// first fill the cell with the background colour and set the right foreground colour
    			if (screen.display[y][x].reverse) {
    				g.setColor( fgColor );
    				g.fillRect( x * charWidth, y * charHeight, charWidth, charHeight );
    				g.setColor( bgColor );
    			} else {
    				if (screen.display[y][x].dim) {
    					g.setColor( dimColor );
    				} else {
    					g.setColor( fgColor );
    				}
    			}
    			
    			// draw the character but handle blinking
    			if (screen.blinking_enabled && screen.blinkState && screen.display[y][x].blink) {
    				g.setColor( bgColor );
    	    		g.fillRect( x * charWidth, (y + 1) * charHeight, charWidth, charHeight );
    			} else {
   					drawChar( g, screen.display[y][x].charValue, x * charWidth, ((y + 1) * charHeight ) );
       			}
    			
    			// underscore
    			if (screen.display[y][x].underscore) {
    				g.drawLine( x * charWidth, (y + 1) * charHeight, (x + 1) * charWidth, (y + 1) * charHeight );
    			}
    		}
    	}
    }
    
    /***
     * Draw a character from the BDF-derived font
     * 
     * @param g
     * @param charValue
     * @param x
     * @param y
     */
    private void drawChar( final Graphics2D g, final byte charValue, final int x, final int y) {
		
    	int asciiCode = (int) charValue;

    	if (asciiCode < bdfFont.map.length && bdfFont.map[asciiCode].loaded) {
    		for (int cy = 0; cy < 12; cy++) {
    			for (int cx = 0; cx < 10; cx++) {
    				if (bdfFont.map[asciiCode].row[cy].get(cx)) {
    					//g.drawLine( x + cx, y - cy, x + cx + 1, y - cy );
    					g.drawLine( x + cx, y - (cy * 2), x + cx + 1, y - (cy * 2) );
    				}
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
