/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap.infrastructure;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author koen
 */
public class SequenceIndexWriter {    
    private final String outputDir;
    private final int overlap;
    private final int readLength;
    private final String INDEX_FILE_NAME = "KA.genome.sequences.gz";
    private BufferedWriter writer;
    
    public SequenceIndexWriter(String outputDir, int overlap, int readLength) throws FileNotFoundException, IOException{
        this.outputDir = outputDir;
        this.overlap = overlap;
        this.readLength = readLength;
        this.writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File(this.outputDir + File.separator + this.INDEX_FILE_NAME)))));
        writer.write("#readLength=" + this.readLength + "\n");
        writer.write("#overlap=" + this.overlap + "\n");
    }
    
    public void printSequence(String chromosome, int position, String sequence) throws IOException{
        writer.write(chromosome + "\t" + position + "\t" + sequence + "\n");
    }
    
    public void close() throws IOException{
        writer.close();
    }
    
    
    @Override
    public void finalize() throws Throwable{
        writer.close();
        super.finalize();
    }
    
}
