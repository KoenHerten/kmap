/*
 * This is GBSX v1.0. A toolkit for experimental design and demultiplexing genotyping by sequencing experiments. \n *  \n * Copyright 2014 KU Leuven
 * 
 * 
 * This file is part of GBSX.
 *
 * GBSX is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * GBSX is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GBSX.  If not, see <http://www.gnu.org/licenses/>.
 */
package kmap.utils.fasta.infractructure;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import kmap.utils.FileLocker;
import kmap.utils.fasta.model.FastaRead;

/**
 *
 * @author Koen Herten for the KU Leuven
 */
public class FastaBufferedReader {
    
    
    private final BufferedReader fastaBufferedReader;
    private FileLocker fileLocker = new FileLocker();
    private final int readAheadLimit = 10000;
    
    /**
     * creates a new FastaBufferedReader of the given file.
     * @param file | the file that must be read
     * @param ziped | boolean if the fasta file is ziped (gz)
     * @throws FileNotFoundException | if the given file isn't found
     * @throws IOException | if an error occures while opening the file
     */
    public FastaBufferedReader(File file, boolean ziped) throws FileNotFoundException, IOException{
        if (ziped){
            this.fastaBufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
        }else{
            this.fastaBufferedReader = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(file))));
        }
        this.fastaBufferedReader.mark(this.readAheadLimit);
    }
    
    
    /**
     * reads the next lines in the fasta file. and returns it as an fastaread
     * @return null if there are no fasta files anymore, else a FastaRead
     * @throws IOException | if any error occures while reading the file
     */
    public FastaRead next() throws IOException{
        if (this.fileLocker.lock()){
            this.fastaBufferedReader.reset();
            String descriptionLine = this.fastaBufferedReader.readLine();
            if (descriptionLine == null){
                this.fileLocker.unlock();
                return null;
            }

            String sequenceLine = this.fastaBufferedReader.readLine();
            sequenceLine.replaceAll("\n", "");
            this.fastaBufferedReader.mark(this.readAheadLimit);
            String extraLine;
            int lineNumber = 1;
            while (! (extraLine = this.fastaBufferedReader.readLine()).contains(">")){
                sequenceLine += extraLine.replaceAll("\n", "");
                this.fastaBufferedReader.mark(this.readAheadLimit);
                System.out.println("" + lineNumber++);
            }
            if (sequenceLine == null){
                this.fileLocker.unlock();
                return null;
            }
            FastaRead fastaRead = new FastaRead(descriptionLine, sequenceLine);
            this.fileLocker.unlock();
            return fastaRead;
        }
        return null;
    }  
    
    
    /**
     * closes this buffered reader
     * @throws IOException 
     */
    public void close() throws IOException{
        this.fileLocker.waitTillCompleteUnlock();
        this.fastaBufferedReader.close();
    }
    
    
    /**
     * closes first the file
     * then execute finilize()
     * @throws Throwable
     */
    @Override
    public void finalize() throws Throwable{
        try {
            this.fastaBufferedReader.close();
        } catch (IOException ex) {
            Logger.getLogger(FastaBufferedReader.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            super.finalize();
        }
    }
    
}
