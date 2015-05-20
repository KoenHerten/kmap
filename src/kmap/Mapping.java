/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import kmap.infrastructure.CompareSequences;
import kmap.infrastructure.IndexReader;
import kmap.infrastructure.MappingQualityCalculator;
import kmap.infrastructure.ReadAndCigarManipulation;
import kmap.infrastructure.SamFileWriter;
import kmap.infrastructure.SequenceIndexReader;
import kmap.infrastructure.SimpleCompareSequences;
import kmap.model.CompareOutput;
import kmap.model.IndexCollection;
import kmap.model.InterestingLocation;
import kmap.model.Location;
import kmap.model.MappingArguments;
import kmap.model.MappingParameters;
import kmap.model.MappingPosition;
import kmap.model.SamRead;
import kmap.utils.fastq.infrastructure.FastqBufferedReader;
import kmap.utils.fastq.model.FastqRead;

/**
 *
 * @author koen
 */
public class Mapping {
    
    private HashMap<String, HashSet<Location>> sequence_location_map;
    private final int kmer;
    private final int readLength;
    private HashMap<String, HashMap<Integer, String>> chromosome_position_sequence_map;
    private final int maxEdit;
    private String samHeader;
    private MappingParameters parameters;
    
    
    public Mapping(MappingParameters parameters) throws IOException{
        this.parameters = parameters;
        this.kmer = this.parameters.getKmer();
        this.readLength = this.parameters.getReadLength();
        this.maxEdit = this.parameters.getMaxEdit();
        if (this.parameters.hasIndexLocation()){
            //index has to be parsed
            IndexReader indexReader = new IndexReader(this.parameters.getIndexLocation());
            indexReader.readIndex();
            this.parameters.setParameter(MappingArguments.KMER, indexReader.getKmer());
            this.sequence_location_map = indexReader.getSequenceLocationMap();
            SequenceIndexReader sequenceIndexReader = new SequenceIndexReader(this.parameters.getIndexLocation());
            sequenceIndexReader.readSequenceIndex();
            this.parameters.setParameter(MappingArguments.OVERLAP, sequenceIndexReader.getOverlap());
            this.parameters.setParameter(MappingArguments.READ_LENGTH, 
                    sequenceIndexReader.getReadLength());
            this.chromosome_position_sequence_map = sequenceIndexReader.getSequenceMap();
            this.samHeader = "@HD\tVN:1.5\tSO:unknown\n";
            BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(this.parameters.getIndexLocation() + File.separator + GenomeIndexing.SAM_HEADER_FILE_NAME)))));
            String line;
            int lineNr = 0;
            while((line = reader.readLine()) != null){
                if (lineNr > 0){
                    this.samHeader += "\n";
                }
                this.samHeader += line;
                lineNr++;
            }
            reader.close();
        }else{
            //index has to be created
            MappingParameters indexParameters = new MappingParameters("index");
            indexParameters.setParameter(MappingArguments.REFERENCE, this.parameters.getReference());
            indexParameters.setParameter(MappingArguments.KMER, this.parameters.getKmer());
            indexParameters.setParameter(MappingArguments.OUTPUT_DIR, this.parameters.getOutputDir());
            GenomeIndexing genomeIndexing = new GenomeIndexing(indexParameters);
            IndexCollection indexCollection = genomeIndexing.createCompleteIndex();
            this.parameters.setParameter(MappingArguments.KMER, indexCollection.getKmer());
            this.sequence_location_map = indexCollection.getSequenceLocationMap();
            this.parameters.setParameter(MappingArguments.OVERLAP, indexCollection.getOverlap());
            this.parameters.setParameter(MappingArguments.READ_LENGTH, indexCollection.getReadLength());
            this.chromosome_position_sequence_map = indexCollection.getChromosomePositionSequenceMap();
            this.samHeader = indexCollection.getSamHeader();
        }
    }
    
    public void map(String fastqFile, boolean isZipped) throws IOException{
        String sampleName = fastqFile.split(File.separator)[fastqFile.split(File.separator).length -1].replace(".fastq", "");
        FastqBufferedReader fastqBufferedReader = new FastqBufferedReader(new File(fastqFile), isZipped);
        FastqRead read;
        int readCount = 0;
        SamFileWriter samFileWriter = new SamFileWriter(this.parameters.getOutputDir(), sampleName);
        samFileWriter.writeLine(this.samHeader);
        while ((read = fastqBufferedReader.next()) != null){
            String sequence = read.getSequence().toLowerCase();
            readCount++;
            if (readCount % 1000 == 0){
                System.out.println("Processed " + readCount + " reads.");
            }
            HashSet<MappingPosition> mappingPositionsSet = new HashSet<>();
            mappingPositionsSet.addAll(this.mapSequence(sequence));
            mappingPositionsSet.addAll(this.mapSequence(ReadAndCigarManipulation.getReverseComplement(sequence)));
            ArrayList<SamRead> samReadList = new ArrayList<>();
            for (MappingPosition mp : mappingPositionsSet){
                MappingQualityCalculator mappingQualityCalculator = new MappingQualityCalculator(this.parameters.getMappingQualityType());
                int mapQ = mappingQualityCalculator.getMappingQuality(mappingPositionsSet.size(), mp, mappingPositionsSet);
                int flag = 0;
                if (mappingPositionsSet.size() > 1){
                    flag += 1;
                }
                if (mp.isReverse()){
                    flag += 8;
                }
                sequence = read.getSequence();
                String quality = read.getQuality();
                if (mp.isReverse()){
                    sequence = ReadAndCigarManipulation.getReverseComplement(read.getSequence());
                    quality = ReadAndCigarManipulation.getReverseQuality(read.getQuality());
                }
                String extra_fields = "AS:i:" + mp.getScore() + "\t"+ "NH:i:" + mp.getEdit();
                SamRead samRead = new SamRead(read.getDescription().substring(1), flag, 
                        mp.getChromosome(), mp.getPosition(), 
                        mapQ, mp.getCigar(), "*", 0, 0, 
                        sequence, quality, extra_fields);
                samReadList.add(samRead);
            }
            if (mappingPositionsSet.isEmpty()){
                //no mapping
                SamRead samRead = new SamRead(read.getDescription().substring(1), 4, "*", 0, 
                        0, read.getSequence().length() + "S", "*", 0, 0, read.getSequence(), read.getQuality());
                samReadList.add(samRead);
            }
            for (SamRead samRead : samReadList){
                //print sam reads in the sam file
                samFileWriter.writeSamLine(samRead);
            }
        }
        
        
        fastqBufferedReader.close();
        samFileWriter.close();
    }
    
    private HashSet<MappingPosition> mapSequence(String sequence){
        HashSet<InterestingLocation> location_set = new HashSet<>();
        //get forward sequence location
        for (int i=0; i <= sequence.length() - this.kmer; i+=this.parameters.getSeedDistance()){
            String sub = sequence.substring(i, i + this.kmer);
            if (this.sequence_location_map.containsKey(sub)){
                for (Location location : this.sequence_location_map.get(sub)){
                    //try to find previous seeds and overlapping locations
                    boolean added =false;
                    for (InterestingLocation interestingLocation : location_set){
                        if (interestingLocation.getChromosome().equals(location.getChromosome())){
                            if (interestingLocation.getPosition() < location.getPosition()
                                    && interestingLocation.getEnd() > location.getPosition()){
                                interestingLocation.changeEnd(location.getPosition() + this.kmer, i);
                                added = true;
                            }
                        }
                    }
                    if (! added){
                        location_set.add(new InterestingLocation(location.getChromosome(), 
                                location.getPosition(), location.getPosition() + this.kmer, i));
                    }
                }
            }else{
                //System.out.println("\tNOT FOUND");
            }
        }
        //locations of the seeds found. Combine the seed locations to find the most interesting places
        HashSet<MappingPosition> mappingPositionsSet = new HashSet<>();
        for (InterestingLocation loc : location_set){
            MappingPosition mappingPosition = this.getInfo(sequence, loc);
            mappingPositionsSet.add(mappingPosition);
        }
        if (mappingPositionsSet.size() > 1){
            HashSet<MappingPosition> newSet = new HashSet<>();
            for(MappingPosition mp : mappingPositionsSet){
                boolean correct = true;
                MappingPosition removeMP = null;
                for(MappingPosition currentMp : newSet){
                    if(currentMp.getChromosome().equals(mp.getChromosome())
                            && currentMp.getPosition() == mp.getPosition()){
                        //same position = so same element, but with different scores, take best mapping one
                        if(currentMp.getScore() > mp.getScore()){
                            //currentMp is better
                            correct = false;
                        }else if(currentMp.getScore() < mp.getScore()){
                            //mp is better
                            correct = true;
                            removeMP = currentMp;
                        }else{
                            //both same score
                            if(currentMp.getEdit() < mp.getEdit()){
                                //currentMp has lower edit
                                correct = false;
                            }else if(currentMp.getEdit() > mp.getEdit()){
                                //mp has lower edit
                                correct = true;
                                removeMP = currentMp;
                            }else{
                                //keep both
                                correct = true;
                            }
                        }
                    }
                }
                if(correct){
                    //correct the list
                    if (removeMP != null){
                        newSet.remove(removeMP);
                    }
                    newSet.add(mp);
                }
            }
            mappingPositionsSet = new HashSet<>(newSet);
        }
        return mappingPositionsSet;
    }
    
    private MappingPosition getInfo(String sequence, InterestingLocation loc){
        String cigar = "";
        int edit = 0;
        double score = 0;
        int begin_position = loc.getPosition();
        if (loc.getEnd() - loc.getPosition() == sequence.length()){
            //exact the same sequence
            for (int i=0; i < sequence.length(); i++){
                cigar += "M";
            }
            edit = 0;
            score = this.parameters.getCorrect_base_score() * sequence.length();
        }else{
            //no exact match
            int reference_index_position = (loc.getPosition() / this.readLength) * this.readLength + Mapper.BASE_INDEX;
            if ((loc.getPosition() - reference_index_position -loc.getSeedStart()) < 0
                    && reference_index_position > 0){
                reference_index_position -= this.readLength;
            }
            if (! this.chromosome_position_sequence_map.containsKey(loc.getChromosome())){
                System.err.println("ERROR: " + loc.getChromosome() + " NOT FOUND");
            }else{
                if (! this.chromosome_position_sequence_map.get(loc.getChromosome()).containsKey(reference_index_position)){
                    System.err.println("ERROR: " + reference_index_position + " POSITION NOT FOUND");
                }
            }
            
            String referenceSequence = this.chromosome_position_sequence_map.get(loc.getChromosome()).get(reference_index_position);
            int reference_index = loc.getPosition() - reference_index_position - (loc.getSeedStart());
            
            if (loc.getSeedStart() != 0){
                //begin is different
                referenceSequence = this.chromosome_position_sequence_map.get(loc.getChromosome()).get(reference_index_position);
                
                String begin_sequence = sequence.substring(0, loc.getSeedStart());
                String begin_reverse = ReadAndCigarManipulation.getReverseComplement(begin_sequence);
                
                int ref_start = loc.getPosition() - reference_index_position - loc.getSeedStart() - this.maxEdit;
                if (ref_start < 0){
                    ref_start = 0;
                }
                String reference_reverse = ReadAndCigarManipulation.getReverseComplement(referenceSequence.substring(ref_start, loc.getPosition() - reference_index_position));
                CompareSequences compareSequences;
                compareSequences = new SimpleCompareSequences(this.parameters);
                CompareOutput compareOutput = compareSequences.compare(reference_reverse, 
                        begin_reverse, this.maxEdit);
                cigar += ReadAndCigarManipulation.getCigarReverse(compareOutput.getCigar());
                edit += compareOutput.getEdit();
                score += compareOutput.getScore();
                begin_position -= compareOutput.getMatchScore();
                
            }
            
            //middle have to match
            for (int i=0; i < (loc.getSeedEnd() - loc.getSeedStart() + this.kmer); i++){
                cigar += "M";
                score += this.parameters.getCorrect_base_score();
                
            }
            
            if (loc.getSeedEnd() + this.kmer != sequence.length()){
                //end is different
                referenceSequence = this.chromosome_position_sequence_map.get(loc.getChromosome()).get(reference_index_position);
                String end_sequence = sequence.substring(loc.getSeedEnd() + this.kmer);
                CompareSequences compareSequences;
                compareSequences = new SimpleCompareSequences(this.parameters);
                CompareOutput compareOutput = compareSequences.compare(
                        referenceSequence.substring(loc.getSeedEnd() + this.kmer, (sequence.length() + this.maxEdit + 1)), 
                        end_sequence, this.maxEdit);
                cigar += compareOutput.getCigar();
                edit += compareOutput.getEdit();
                score += compareOutput.getScore();
            }
        }
        MappingPosition mappingPosition = new MappingPosition(sequence, cigar, 
                loc.getChromosome(), begin_position, edit, score);
        
        return mappingPosition;
    }
    
    
}


