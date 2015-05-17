/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap.infrastructure;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author koen
 */
public class SequenceIndexReader {
    
    
    private final String inputIndexFile;
    private int overlap;
    private int readLength;
    private final String INDEX_FILE_NAME = "KA.genome.sequences.gz";
    private HashMap<String, HashMap<Integer, String>> chromosome_position_sequence_map;
    
    public SequenceIndexReader(String inputIndexFile){
        this.inputIndexFile = inputIndexFile;
    }
    
    public void readSequenceIndex() throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(this.inputIndexFile + File.separator + this.INDEX_FILE_NAME)))));
        this.chromosome_position_sequence_map = new HashMap<>();
        String line;
        int sequence_count = 0;
        while ((line = reader.readLine()) != null){
            if (line.contains("#readLength=")){
                this.readLength = Integer.parseInt(line.replace("#readLength=", ""));
                System.out.println("ReadLength found: " + this.readLength);
            }else if (line.contains("#overlap=")){
                this.overlap = Integer.parseInt(line.replace("#overlap=", ""));
                System.out.println("Overlap found: " + this.overlap);
            }else{
                sequence_count++;
                if (sequence_count % 10000 == 0){
                    System.out.println("Read " + sequence_count + " number of sequences");
                }
                String[] parts = line.split("\t");
                String chromosome = parts[0];
                int position = Integer.parseInt(parts[1]);
                String sequence = parts[2];
                if (! this.chromosome_position_sequence_map.containsKey(chromosome)){
                    this.chromosome_position_sequence_map.put(chromosome, new HashMap<>());
                }
                this.chromosome_position_sequence_map.get(chromosome).put(position, sequence);
            }
        }
        reader.close();
    }
    
    public int getOverlap(){
        return this.overlap;
    }
    
    public int getReadLength(){
        return this.readLength;
    }
    
    public HashMap<String, HashMap<Integer, String>> getSequenceMap(){
        return this.chromosome_position_sequence_map;
    }
}
