package components;

/***
 * Version 0.5 Add Constants and sync with v.0.4
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.BitSet;

public final class BDFfont {

	public static final int CHAR_PIXEL_WIDTH  = 10;
	public static final int CHAR_PIXEL_HEIGHT = 12;

	public int charCount;
	public CharBitMap map[];
	public boolean loaded;

	class CharBitMap {
		boolean loaded;
		BitSet row[];

		CharBitMap() {
			row = new BitSet[CHAR_PIXEL_HEIGHT];
			for (int i = 0; i < CHAR_PIXEL_HEIGHT; i++) {
				row[i] = new BitSet( CHAR_PIXEL_WIDTH );
			}
		}
	}

	public BDFfont() {
		map = new CharBitMap[128];
		for (int i = 0; i < 128; i++) {
			map[i] = new CharBitMap();
		}
		loaded = false;
	}

	public boolean load( InputStream  fontFileStream ) {

		BufferedReader bfr;
		bfr = new BufferedReader(  new InputStreamReader( fontFileStream )  );


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

				// load the actual bitmap for this char a row at a time from the top down
				for (int bitMapLine = pixHeight - 1; bitMapLine >= 0; bitMapLine--) {
					String lineStr = bfr.readLine();
					byte lineByte = (byte) ( Integer.parseInt( lineStr, 16 ) );
					for (int i=0; i < pixWidth; i++) {
						boolean pix = ((lineByte & 0x80) >> 7) == 1; // test the MSB
						map[asciiCode].row[bitMapLine + yOffset].set( xOffset + i, pix ); // FIXME: !!!
						lineByte = (byte) (lineByte << 1);
					}
				}
				map[asciiCode].loaded = true;


			}
		} catch (IOException e) {
			// e.printStackTrace();
			return false;
		}
		loaded = true;
		return true;
	}

}
