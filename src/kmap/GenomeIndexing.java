/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import kmap.infrastructure.IndexWriter;
import kmap.infrastructure.SequenceIndexWriter;
import kmap.model.IndexCollection;
import kmap.model.Location;
import kmap.model.MappingParameters;

/**
 *
 * @author koen
 */
public class GenomeIndexing {
    
    public static final String SAM_HEADER_FILE_NAME = "KA.genome.header.gz";
    private MappingParameters parameters;
    
    public GenomeIndexing(MappingParameters parameters){
        this.parameters = parameters;
    }
    
    public void createAndSaveIndex() throws IOException{
        //get the index collection
        IndexCollection indexCollection = this.createCompleteIndex();
        //create the header of the sam file
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File(parameters.getOutputDir() + File.separator + this.SAM_HEADER_FILE_NAME)))));
        ArrayList<String> chr_list = new ArrayList<>(indexCollection.getChromosomeLengthMap().keySet());
        Collections.sort(chr_list);
        for (String chr : chr_list){
            writer.write("@SQ\tSN:" + chr + "\tLN:" + indexCollection.getChromosomeLengthMap().get(chr));
        }
        writer.close();
        
        //write the index
        
        IndexWriter indexWriter = new IndexWriter(this.parameters.getOutputDir(), this.parameters.getKmer());
        indexWriter.printIndex(indexCollection.getSequenceLocationMap());
        
        //write the reference parts
        SequenceIndexWriter sequenceIndexWriter = new SequenceIndexWriter(this.parameters.getOutputDir(), this.parameters.getOverlap(), this.parameters.getReadLength());
        for (String chromosome : indexCollection.getChromosomePositionSequenceMap().keySet()){
            for (int position : indexCollection.getChromosomePositionSequenceMap().get(chromosome).keySet()){
                String sequence = indexCollection.getChromosomePositionSequenceMap().get(chromosome).get(position);
                sequenceIndexWriter.printSequence(chromosome, position, sequence);
            }
        }
        sequenceIndexWriter.close();
    }
    
    public IndexCollection createCompleteIndex() throws IOException{
        
        BufferedReader fastaBufferedReader;
        File file = new File(this.parameters.getReference());
        if (this.parameters.isZippedReference()){
            fastaBufferedReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(file))));
        }else{
            fastaBufferedReader = new BufferedReader(new InputStreamReader(new DataInputStream(new FileInputStream(file))));
        }
        
        String line;
        String chromosome = "";
        String previousLine = "";
        String sequence = "";
        int position = Mapper.BASE_INDEX;
        int positionForHash = Mapper.BASE_INDEX;
        HashMap<String, HashSet<Location>> sequence_location_map = new HashMap<>();
        HashMap<String, Integer> chromosome_length_map = new HashMap<>();
        HashMap<String, HashMap<Integer, String>> chromosome_position_sequence_map = new HashMap<>();
        while ((line = fastaBufferedReader.readLine()) != null){
            if (line.contains(">")){
                if (! chromosome.equals("")){
                    chromosome_length_map.put(chromosome, position + this.parameters.getKmer());
                }
                chromosome = line.split(" ")[0].replace(">", "");
                previousLine = "";
                position = Mapper.BASE_INDEX;
                System.out.println("Start chromosome " + chromosome);
                if (! chromosome.equals("")){
                    if (! chromosome_position_sequence_map.containsKey(chromosome)){
                        chromosome_position_sequence_map.put(chromosome, new HashMap<>());
                    }
                    chromosome_position_sequence_map.get(chromosome).put(position, sequence);
                }
                chromosome = line.split(" ")[0].replace(">", "");
                sequence = "";
                positionForHash = Mapper.BASE_INDEX;
                System.out.println("Start chromosome " + chromosome);
            }else{
                sequence += line.toLowerCase();
                while (sequence.length() > (this.parameters.getReadLength() + this.parameters.getOverlap())){
                    //print sequence
                    if (! chromosome_position_sequence_map.containsKey(chromosome)){
                        chromosome_position_sequence_map.put(chromosome, new HashMap<>());
                    }
                    chromosome_position_sequence_map.get(chromosome).put(position, sequence);
                    sequence = sequence.substring(this.parameters.getReadLength());
                    position += this.parameters.getReadLength();
                }
                String sequenceLine = line;
                if (previousLine.length() - this.parameters.getKmer() > 0){
                    sequenceLine = previousLine.substring(previousLine.length() - this.parameters.getKmer() +1) + line;
                    positionForHash = positionForHash;
                }
                //System.out.println("" + sequence);
                if (positionForHash % 10000 == 0){
                    System.out.println("\tProcessed " + positionForHash + " bp");
                }
                for (int i = 0; i <= sequenceLine.length() - this.parameters.getKmer(); i++){
                    String sub = sequenceLine.substring(i, i + this.parameters.getKmer()).toLowerCase();
                    if (! sub.contains("n")){
                        //interesting subsequence (kmer)
                        Location location = new Location(chromosome, positionForHash + i);
                        if (sequence_location_map.containsKey(sub)){
                            sequence_location_map.get(sub).add(location);
                        }else{
                            sequence_location_map.put(sub, new HashSet<>());
                            sequence_location_map.get(sub).add(location);
                        }
                    }
                }
                //positionForHash += line.length();
                positionForHash += (sequenceLine.length() -this.parameters.getKmer() +1);
                previousLine = line;
            }
        }
        
        if (! chromosome.equals("")){
            chromosome_length_map.put(chromosome, position + this.parameters.getKmer());
        }
        fastaBufferedReader.close();
            
        System.out.println("Number of Seqs: " + sequence_location_map.keySet().size());
        int total = 0;
        for (String sub : sequence_location_map.keySet()){
            total += sequence_location_map.get(sub).size();
        }
        System.out.println("Number of Locations: " + total);
        
        IndexCollection indexCollection = new IndexCollection(sequence_location_map, 
                chromosome_length_map, chromosome_position_sequence_map, 
                this.parameters.getKmer(), this.parameters.getOverlap(), this.parameters.getReadLength());
        return indexCollection;
    }
}
