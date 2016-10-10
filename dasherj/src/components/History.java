/*
 * Copyright (C) 2016 Stephen Merrony
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package components;

import java.util.LinkedList;

/**
 * This class represents the terminal history which is used for scroll-back purposes.
 *
 * @author Stephen Merrony
 * 
 * v.1.2 - Class introduced
 *       - Remove unused import, increase history to 2000 lines
 */
public class History {
    
    public static final int MAX_HISTORY_LINES = 2000;
    
    /**
     * the buffer is a list of arrays of Cells...
     */
    private final LinkedList<Cell[]> buffer;

    public History() {
        buffer = new LinkedList<>();
    }
    
    public void addLine( Cell[] cells ) {
        if (buffer.size() == MAX_HISTORY_LINES) {
            buffer.remove();
        }
        Cell[] cellLine = new Cell[Terminal.TOTAL_COLS];
        for ( int c = 0; c < cells.length; c++ ) {
            cellLine[c] = new Cell();
            cellLine[c].copy(  cells[c] );
        }
        buffer.add( cellLine ); 
    }
    
    public int lineCount() {
        return buffer.size();
    }
    
    public String fetchAllAsString() {
        String text = "(History Empty)";
        StringBuilder builder = new StringBuilder( 1000 );
        Cell[] lineOfCells;
        for (int l = 0; l < buffer.size(); l++) {
            lineOfCells = buffer.get( l );
            for( Cell cell : lineOfCells ) {
                builder.append( (char) cell.charValue );
            }
            builder.append( "\n" );
        }
        if (builder.length() > 0) text = builder.toString();
        return text;
    }
    
}
