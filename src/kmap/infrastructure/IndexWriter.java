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
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPOutputStream;
import kmap.model.Location;

/**
 *
 * @author koen
 */
public class IndexWriter {
    
    private final String outputDir;
    private final int kmer;
    public static final String INDEX_FILE_NAME = "KA.genome.index.gz";
    
    public IndexWriter(String outputDir, int kmer){
        this.outputDir = outputDir;
        this.kmer = kmer;
    }
    
    public void printIndex(HashMap<String, HashSet<Location>> sequence_location_map) throws FileNotFoundException, IOException{
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File(this.outputDir + File.separator + this.INDEX_FILE_NAME)))));
        
        writer.write("#Kmer=" + this.kmer + "\n");
        
        for (String subSeq : sequence_location_map.keySet()){
            for (Location location : sequence_location_map.get(subSeq)){
                String line = "" + subSeq + "\t" + location.getChromosome() + "\t" + location.getPosition();
                writer.write(line +  "\n");
            }
        }
        writer.close();
    }
}
