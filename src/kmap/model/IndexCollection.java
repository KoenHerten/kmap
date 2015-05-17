/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author koen
 */
public class IndexCollection {
    
    
    private final HashMap<String, HashSet<Location>> sequence_location_map;
    private final HashMap<String, Integer> chromosome_length_map;
    private final HashMap<String, HashMap<Integer, String>> chromosome_position_sequence_map;
    private final int kmer;
    private final int overlap;
    private final int readLength;
    
    public IndexCollection(HashMap<String, HashSet<Location>> sequence_location_map, 
            HashMap<String, Integer> chromosome_length_map, 
            HashMap<String, HashMap<Integer, String>> chromosome_position_sequence_map,
            int kmer,
            int overlap,
            int readLength){
        this.chromosome_length_map = chromosome_length_map;
        this.chromosome_position_sequence_map = chromosome_position_sequence_map;
        this.sequence_location_map = sequence_location_map;
        this.kmer = kmer;
        this.overlap = overlap;
        this.readLength = readLength;
    }
    
    public HashMap<String, HashSet<Location>> getSequenceLocationMap(){
        return this.sequence_location_map;
    }
    
    public HashMap<String, Integer> getChromosomeLengthMap(){
        return this.chromosome_length_map;
    }
    
    public HashMap<String, HashMap<Integer, String>> getChromosomePositionSequenceMap(){
        return this.chromosome_position_sequence_map;
    }
    
    public String getSamHeader(){
        ArrayList<String> chr_list = new ArrayList<>(chromosome_length_map.keySet());
        Collections.sort(chr_list);
        String samHeader = "@HD\tVN:1.5\tSO:unknown";
        for (String chr : chr_list){
            samHeader += "\n@SQ\tSN:" + chr + "\tLN:" + chromosome_length_map.get(chr) + "";
        }
        return samHeader;
    }
    
    public int getKmer(){
        return this.kmer;
    }
    
    public int getOverlap(){
        return this.overlap;
    }
    
    public int getReadLength(){
        return this.readLength;
    }
    
}
