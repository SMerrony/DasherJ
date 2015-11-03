package components;

/***
 * Version 0.6 Switch to Raster for chars
 * Version 0.5 Add Constants and sync with v.0.4
 */

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class BDFfont {

	public static final int CHAR_PIXEL_WIDTH  = 10;
	public static final int CHAR_PIXEL_HEIGHT = 12;
	public static final int CHARSET_SIZE = 128; 

	public int charCount;
	public BufferedImage charImages[], charDimImages[], charReverseImages[];
	public boolean charLoaded[];
	public boolean loaded;

	public BDFfont() {
		charImages        = new BufferedImage[CHARSET_SIZE];
		charDimImages     = new BufferedImage[CHARSET_SIZE];
		charReverseImages = new BufferedImage[CHARSET_SIZE];
		charLoaded        = new boolean[CHARSET_SIZE];	
		for (int i = 0; i < CHARSET_SIZE; i++) {
			charImages[i]        = new BufferedImage( CHAR_PIXEL_WIDTH, CHAR_PIXEL_HEIGHT, BufferedImage.TYPE_BYTE_GRAY);
			charDimImages[i]     = new BufferedImage( CHAR_PIXEL_WIDTH, CHAR_PIXEL_HEIGHT, BufferedImage.TYPE_BYTE_GRAY );
			charReverseImages[i] = new BufferedImage( CHAR_PIXEL_WIDTH, CHAR_PIXEL_HEIGHT, BufferedImage.TYPE_BYTE_GRAY );
		}
		loaded = false;
	}

	public boolean load( InputStream  fontFileStream ) {

		BufferedReader bfr;
		bfr = new BufferedReader(  new InputStreamReader( fontFileStream )  );
		WritableRaster raster, dimRaster, reverseRaster;

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
				
				raster = charImages[asciiCode].getRaster();
				dimRaster = charDimImages[asciiCode].getRaster();
				reverseRaster = charReverseImages[asciiCode].getRaster();
				// fill the reverse raster with whiteness
				for (int x = 0; x < CHAR_PIXEL_WIDTH; x++)
					for (int y = 0; y < CHAR_PIXEL_HEIGHT; y++)
						reverseRaster.setSample( x, y, 0, 255 ); 
				
				// load the actual bitmap for this char a row at a time from the top down
				for (int bitMapLine = pixHeight - 1; bitMapLine >= 0; bitMapLine--) {
					String lineStr = bfr.readLine();
					byte lineByte = (byte) ( Integer.parseInt( lineStr, 16 ) );
					for (int i=0; i < pixWidth; i++) {
						boolean pix = ((lineByte & 0x80) >> 7) == 1; // test the MSB
						int thisYoffset = CHAR_PIXEL_HEIGHT - (1 + bitMapLine + yOffset);
						raster.setSample( xOffset + i, thisYoffset, 0, pix ? 255 : 0 );
						reverseRaster.setSample( xOffset + i, thisYoffset, 0, pix ? 0 : 255 );
						dimRaster.setSample( xOffset + i, thisYoffset, 0, pix ? 127 : 0 );
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
