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
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import kmap.model.Location;

/**
 *
 * @author koen
 */
public class IndexReader {
    
    private final String inputIndexFile;
    private int kmer;
    public static final String INDEX_FILE_NAME = "KA.genome.index.gz";
    private HashMap<String, HashSet<Location>> sequence_location_map;
    
    public IndexReader(String inputIndexFile){
        this.inputIndexFile = inputIndexFile;
    }
    
    public void readIndex() throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(this.inputIndexFile + File.separator + this.INDEX_FILE_NAME)))));
        this.sequence_location_map = new HashMap<>();
        String line;
        int kmer_count = 0;
        while ((line = reader.readLine()) != null){
            if (line.contains("#Kmer=")){
                this.kmer = Integer.parseInt(line.replace("#Kmer=", ""));
                System.out.println("Kmer found: " + this.kmer);
            }else{
                kmer_count++;
                if (kmer_count % 10000 == 0){
                    System.out.println("Read " + kmer_count + " number of kmers");
                }
                String[] parts = line.split("\t");
                Location location = new Location(parts[1], Integer.parseInt(parts[2]));
                if (this.sequence_location_map.containsKey(parts[0])){
                    this.sequence_location_map.get(parts[0]).add(location);
                }else{
                    this.sequence_location_map.put(parts[0], new HashSet<>());
                    this.sequence_location_map.get(parts[0]).add(location);
                }
            }
        }
        reader.close();
    }
    
    public int getKmer(){
        return this.kmer;
    }
    
    public HashMap<String, HashSet<Location>> getSequenceLocationMap(){
        return this.sequence_location_map;
    }
}
