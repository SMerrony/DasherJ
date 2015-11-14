package components;

/***
 * The custom font used by DasherJ
 * 
 * Default colours are picked up from the Crt object.
 * 
 * Version 0.9 Switch to JavaFX from Swing
 * Version 0.6 Switch to Raster for chars
 * Version 0.5 Add Constants and sync with v.0.4
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

public final class BDFfont {

	public static final int CHAR_PIXEL_WIDTH  = 10;
	public static final int CHAR_PIXEL_HEIGHT = 12;
	public static final int CHARSET_SIZE = 128; 

	public int charCount;
	public WritableImage charImages[], charDimImages[], charReverseImages[];
	public boolean charLoaded[];
	public boolean loaded;

	public BDFfont() {
		charImages        = new WritableImage[CHARSET_SIZE];
		charDimImages     = new WritableImage[CHARSET_SIZE];
		charReverseImages = new WritableImage[CHARSET_SIZE];
		charLoaded        = new boolean[CHARSET_SIZE];	
		for (int i = 0; i < CHARSET_SIZE; i++) {
			charImages[i]        = new WritableImage( CHAR_PIXEL_WIDTH, CHAR_PIXEL_HEIGHT );
			charDimImages[i]     = new WritableImage( CHAR_PIXEL_WIDTH, CHAR_PIXEL_HEIGHT );
			charReverseImages[i] = new WritableImage( CHAR_PIXEL_WIDTH, CHAR_PIXEL_HEIGHT );
		}
		loaded = false;
	}

	public boolean load( InputStream  fontFileStream ) {

		BufferedReader bfr;
		bfr = new BufferedReader(  new InputStreamReader( fontFileStream )  );
		PixelWriter plainWriter, dimWriter, reverseWriter;

		try {
			while (!(bfr.readLine()).equals( "ENDPROPERTIES" )); // skip over header
			String charCountLine = bfr.readLine();
			if (!charCountLine.startsWith( "CHARS" )) return false;
			charCount = Integer.parseInt( charCountLine.substring( charCountLine.indexOf(' ') + 1 ) );

			for (int cc = 0; cc < charCount; cc++) {
				// skip to start of char
				while (!(bfr.readLine()).startsWith( "STARTCHAR" ));
				String encodingLine = bfr.readLine();
				if (!encodingLine.startsWith( "ENCODING" )) return false;
				int asciiCode = Integer.parseInt( encodingLine.substring( encodingLine.indexOf( ' ' ) + 1 ) );
				// System.out.printf( "BDFfont: Debug - loading character %d\n", asciiCode );

				// skip two lines (SWIDTH & DWIDTH)
				bfr.readLine(); bfr.readLine();

				// decode the BBX line
				String bbxLine = bfr.readLine();
				if (!bbxLine.startsWith( "BBX" )) return false;
				String[] bbxTokens = bbxLine.split( " " );
				int pixWidth = Integer.parseInt( bbxTokens[1]) ; 
				int pixHeight = Integer.parseInt( bbxTokens[2] ); // rows up from base used
				int xOffset = Integer.parseInt( bbxTokens[3] );
				int yOffset = Integer.parseInt( bbxTokens[4] );

				// skip the BITMAP line
				bfr.readLine();
				
				plainWriter = charImages[asciiCode].getPixelWriter();
				dimWriter = charDimImages[asciiCode].getPixelWriter();
				reverseWriter = charReverseImages[asciiCode].getPixelWriter();
				// fill the reverse raster with whiteness
				for (int x = 0; x < CHAR_PIXEL_WIDTH; x++)
					for (int y = 0; y < CHAR_PIXEL_HEIGHT; y++)
						reverseWriter.setColor(x, y, Crt.DFLT_FG_COLOR );
				
				// load the actual bitmap for this char a row at a time from the top down
				for (int bitMapLine = pixHeight - 1; bitMapLine >= 0; bitMapLine--) {
					String lineStr = bfr.readLine();
					byte lineByte = (byte) ( Integer.parseInt( lineStr, 16 ) );
					for (int i=0; i < pixWidth; i++) {
						boolean pix = ((lineByte & 0x80) >> 7) == 1; // test the MSB
						int thisYoffset = CHAR_PIXEL_HEIGHT - (1 + bitMapLine + yOffset);
						plainWriter.setColor( xOffset + i, thisYoffset, pix ? Crt.DFLT_FG_COLOR : Crt.DFLT_BG_COLOR );
						reverseWriter.setColor( xOffset + i, thisYoffset, pix ? Crt.DFLT_BG_COLOR : Crt.DFLT_FG_COLOR );
						dimWriter.setColor( xOffset + i, thisYoffset, pix ? Crt.DFLT_DIM_COLOR : Crt.DFLT_BG_COLOR );
						lineByte = (byte) (lineByte << 1);
					}
				}
				charLoaded[asciiCode] = true;
			}
		} catch (IOException e) {
			// e.printStackTrace();
			return false;
		}
		loaded = true;
		return true;
	}

}
