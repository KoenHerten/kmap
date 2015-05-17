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
import kmap.infrastructure.IndexReader;
import kmap.infrastructure.ReadAndCigarManipulation;
import kmap.infrastructure.SamFileWriter;
import kmap.infrastructure.SequenceIndexReader;
import kmap.model.IndexCollection;
import kmap.model.InterestingLocation;
import kmap.model.Location;
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
    private int kmer;
    private int seedDistance = 1;
    private int overlap;
    private int readLength;
    private HashMap<String, HashMap<Integer, String>> chromosome_position_sequence_map;
    private int correct_base_score = 1;
    private int mismatch_base_score = -1;
    private int indel_score = -2;
    private int maxEdit = 4;
    private String outputDir;
    private String indexLocation;
    private String samHeader;
    private static final String MAPPING_QUALITY_TYPE = "bwa-like";
    
    public Mapping(String indexLocation, String outputDir) throws IOException{
        //read index
        this.indexLocation = indexLocation;
        this.outputDir = outputDir;
        IndexReader indexReader = new IndexReader(indexLocation);
        indexReader.readIndex();
        this.kmer = indexReader.getKmer();
        this.sequence_location_map = indexReader.getSequenceLocationMap();
        SequenceIndexReader sequenceIndexReader = new SequenceIndexReader(indexLocation);
        sequenceIndexReader.readSequenceIndex();
        this.overlap = sequenceIndexReader.getOverlap();
        this.readLength = sequenceIndexReader.getReadLength();
        this.chromosome_position_sequence_map = sequenceIndexReader.getSequenceMap();
        this.samHeader = "@HD\tVN:1.5\tSO:unknown\n";
        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(this.indexLocation + File.separator + GenomeIndexing.SAM_HEADER_FILE_NAME)))));
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
    }
    
    public Mapping(String fastaFile, int kmer, String outputDir) throws IOException{
        //read index
        this.indexLocation = "";
        this.outputDir = outputDir;
        GenomeIndexing genomeIndexing = new GenomeIndexing(fastaFile, kmer, false, outputDir);
        IndexCollection indexCollection = genomeIndexing.createCompleteIndex();
        this.kmer = indexCollection.getKmer();
        this.sequence_location_map = indexCollection.getSequenceLocationMap();
        this.overlap = indexCollection.getOverlap();
        this.readLength = indexCollection.getReadLength();
        this.chromosome_position_sequence_map = indexCollection.getChromosomePositionSequenceMap();
        this.samHeader = indexCollection.getSamHeader();
    }
    
    public void map(String fastqFile, boolean isZipped) throws IOException{
        String sampleName = fastqFile.split(File.separator)[fastqFile.split(File.separator).length -1].replace(".fastq", "");
        FastqBufferedReader fastqBufferedReader = new FastqBufferedReader(new File(fastqFile), isZipped);
        FastqRead read;
        int readCount = 0;
        SamFileWriter samFileWriter = new SamFileWriter(this.outputDir, sampleName);
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
                int mapQ = this.getMappingQuality(mappingPositionsSet.size(), mp, mappingPositionsSet);
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
        for (int i=0; i <= sequence.length() - this.kmer; i+=this.seedDistance){
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
        int score = 0;
        int begin_position = loc.getPosition();
        if (loc.getEnd() - loc.getPosition() == sequence.length()){
            //exact the same sequence
            for (int i=0; i < sequence.length(); i++){
                cigar += "M";
            }
            edit = 0;
            score = this.correct_base_score * sequence.length();
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
                HashMap<String, String> edit_map = this.compare(reference_reverse, 
                        begin_reverse, this.maxEdit);
                cigar += ReadAndCigarManipulation.getCigarReverse(edit_map.get("cigar"));
                edit += Integer.parseInt(edit_map.get("edit"));
                score += Integer.parseInt(edit_map.get("score"));
                begin_position -= Integer.parseInt(edit_map.get("match_score"));
                
            }
            
            //middle have to match
            for (int i=0; i < (loc.getSeedEnd() - loc.getSeedStart() + this.kmer); i++){
                cigar += "M";
                score += this.correct_base_score;
                
            }
            
            if (loc.getSeedEnd() + this.kmer != sequence.length()){
                //end is different
                referenceSequence = this.chromosome_position_sequence_map.get(loc.getChromosome()).get(reference_index_position);
                String end_sequence = sequence.substring(loc.getSeedEnd() + this.kmer);
                HashMap<String, String> edit_map = this.compare(
                        referenceSequence.substring(loc.getSeedEnd() + this.kmer, (sequence.length() + this.maxEdit + 1)), 
                        end_sequence, this.maxEdit);
                cigar += edit_map.get("cigar");
                edit += Integer.parseInt(edit_map.get("edit"));
                score += Integer.parseInt(edit_map.get("score"));
            }
        }
        MappingPosition mappingPosition = new MappingPosition(sequence, cigar, 
                loc.getChromosome(), begin_position, edit, score);
        
        return mappingPosition;
    }
    
    private HashMap<String, String> compare(String reference, String sequence, int differences){
        //search for mismatches first
        String cigar = "";
        int score = 0;
        int edit = 0;
        int match_score = 0;
        int change_score = 0;
        if (sequence.length() <= reference.length() && sequence.equals(reference.substring(0, sequence.length()))){
            //complete match
            for (int i=0; i < sequence.length(); i++){
                cigar += "M";
            }
            score = this.correct_base_score * sequence.length();
            edit = 0;
            match_score = sequence.length();
            change_score = 0;
        }else if(differences < 0 || reference.length() == 0){
            //no differences allowed any more => soft clipping
            for (int i =0; i < sequence.length(); i++){
                cigar = cigar + "S";
            }
            score = 0;
            edit = 0;
            match_score = 0;
            change_score = 0;
        }else{
            //try new, first check if base is equal
            HashMap<String, String> result_map;
            //mismatch or same
            String mismatch_cigar;
            int mismatch_score;
            int mismatch_edit;
            int mismatch_match_score;
            int mismatch_dif = 0;
            if (sequence.charAt(0) != reference.charAt(0)){
                mismatch_dif = 1;
            }
            result_map = this.compare(reference.substring(1), sequence.substring(1), 
                    differences - mismatch_dif);
            mismatch_match_score = Integer.parseInt(result_map.get("match_score")) + 1;
            int mismatch_change_score = 0;
            if (sequence.charAt(0) == reference.charAt(0)){
                cigar = "M" + result_map.get("cigar");
                score = Integer.parseInt(result_map.get("score")) + this.correct_base_score;
                edit = Integer.parseInt(result_map.get("edit"));
                match_score = mismatch_match_score;
                mismatch_change_score = Integer.parseInt(result_map.get("change_score"));
            }else{
                mismatch_cigar = "X" + result_map.get("cigar");
                mismatch_score = Integer.parseInt(result_map.get("score")) + this.mismatch_base_score;
                mismatch_edit = Integer.parseInt(result_map.get("edit")) + 1;
                mismatch_change_score = Integer.parseInt(result_map.get("change_score")) + 1;
                match_score = mismatch_match_score;
                cigar = mismatch_cigar;
                score = mismatch_score;
            
                //if (mismatch_score < (this.correct_base_score * sequence.length() - this.maxEdit * this.mismatch_base_score)){
                if(match_score != sequence.length()){
                    //insert
                    String insert_cigar;
                    int insert_score;
                    int insert_edit;
                    int insert_match_score;
                    int insert_change_score;
                    if (reference.length() == sequence.length()){
                        insert_cigar = "";
                        for (int i=0; i < reference.length(); i++){
                            insert_cigar += "S";
                        }
                        insert_edit = 0;
                        insert_score = 0;
                        insert_match_score = 0;
                        insert_change_score = 0;
                    }else{
                        result_map = this.compare(reference, sequence.substring(1), differences -1);
                        insert_cigar = "I" + result_map.get("cigar");
                        insert_score = Integer.parseInt(result_map.get("score")) + this.indel_score;
                        insert_edit = Integer.parseInt(result_map.get("edit")) + 1;
                        //insert stays on reference position
                        insert_match_score = Integer.parseInt(result_map.get("match_score"));
                        insert_change_score = Integer.parseInt(result_map.get("change_score")) + 1;
                    }
                    //delete
                    String delete_cigar;
                    int delete_score;
                    int delete_edit;
                    int delete_match_score;
                    int delete_change_score;
                    if (reference.length() == sequence.length()){
                        delete_cigar = "";
                        for(int i=0; i < reference.length(); i++){
                            delete_cigar += "S";
                        }
                        delete_edit = 0;
                        delete_score = 0;
                        delete_match_score = 0;
                        delete_change_score = 0;
                    }else{
                        result_map = this.compare(reference.substring(1), sequence, differences -1);
                        delete_cigar = "D" + result_map.get("cigar");
                        delete_score = Integer.parseInt(result_map.get("score")) + this.indel_score;
                        delete_edit = Integer.parseInt(result_map.get("edit")) + 1;
                        //delete skips reference position
                        delete_match_score = Integer.parseInt(result_map.get("match_score")) + 1;
                        delete_change_score = Integer.parseInt(result_map.get("change_score")) + 1;
                    }
                    
                    boolean isMismatch = false;
                    boolean isInsert = false;
                    boolean isDelete = false;
                    int mismatch_correct = 0;
                    int insert_correct = 0;
                    int delete_correct = 0;
                    if (mismatch_cigar.contains("D")){
                        for (char c : mismatch_cigar.toCharArray()){
                            if (c == 'D') mismatch_correct++;
                        }
                    }
                    if (insert_cigar.contains("D")){
                        for (char c : insert_cigar.toCharArray()){
                            if (c == 'D') insert_correct++;
                        }
                    }
                    if (delete_cigar.contains("D")){
                        for (char c : delete_cigar.toCharArray()){
                            if (c == 'D') delete_correct++;
                        }
                    }
                    if ((mismatch_match_score - mismatch_correct) > (insert_match_score - insert_correct)
                            && (mismatch_match_score - mismatch_correct) > (delete_match_score - delete_correct)){
                        //MISMATCH score is the highest
                        isMismatch = true;
                    }else if((insert_match_score - insert_correct) > (mismatch_match_score - mismatch_correct) 
                            && (insert_match_score - insert_correct) > (delete_match_score - delete_correct)){
                        //INSERT score is the highest
                        isInsert = true;
                    }else if((delete_match_score - delete_correct) > (mismatch_match_score - mismatch_correct) 
                            && (delete_match_score - delete_correct) > (insert_match_score - insert_correct)){
                        //DELETE score is the highest
                        isDelete = true;
                    }else if((mismatch_match_score - mismatch_correct) == (insert_match_score - insert_correct) 
                            && (mismatch_match_score - mismatch_correct) > (delete_match_score - delete_correct)){
                        //MISMATCH and INSERT equals
                        if (mismatch_edit < insert_edit){
                            //MISMATCH edit lowest
                            isMismatch = true;
                        }else if(mismatch_edit > insert_edit){
                            //INSERT edit lowest
                            isInsert = true;
                        }else{
                            //MISMATCH and INSERT edit equals => MISMATCH
                            isMismatch = true;
                        }
                    }else if((mismatch_match_score - mismatch_correct) == (delete_match_score - delete_correct) 
                            && (mismatch_match_score - mismatch_correct) > (insert_match_score - insert_correct)){
                        //MISMATCH and DELETE equals
                        if (mismatch_edit < delete_edit){
                            //MISMATCH edit lowest
                            isMismatch = true;
                        }else if(mismatch_edit > delete_edit){
                            //DELETE edit lowest
                            isDelete = true;
                        }else{
                            //MISMATCH and DELETE edit equalse => MISMATCH
                            isMismatch = true;
                        }
                    }else if((insert_match_score - insert_correct) == (delete_match_score - delete_correct) 
                            && (mismatch_match_score - mismatch_correct) > (delete_match_score - delete_correct)){
                        //INSERT and DELETE equals
                        if (insert_edit < delete_edit){
                            //INSERT edit lowest
                            isInsert = true;
                        }else if(insert_edit > delete_edit){
                            //DELETE edit lowest
                            isDelete = true;
                        }else{
                            //INSERT and DELETE edit equals => INSERT?
                            isInsert = true;
                        }
                    }else{
                        //MISMATCH and INSERT and DELETE equals
                        if (mismatch_edit <= insert_edit && mismatch_edit <= delete_edit){
                            //MISMATCH
                            isMismatch = true;
                        }else if(insert_edit < mismatch_edit && insert_edit <= delete_edit){
                            //INSERT
                            isInsert = true;
                        }else if(delete_edit < mismatch_edit && delete_edit < insert_edit){
                            //DELETE
                            isDelete = true;
                        }else{
                            //MISMATCH?
                            isMismatch = true;
                        }
                    }
                    
                    
                    if (isMismatch){
                        //mismatch edit is lower, so mismatch
                        cigar = mismatch_cigar;
                        score = mismatch_score;
                        edit = mismatch_edit;
                        match_score = mismatch_match_score;
                        change_score = mismatch_change_score;
                    }else if(isInsert){
                        //insert is lower, so insert
                        cigar = insert_cigar;
                        score = insert_score;
                        edit = insert_edit;
                        match_score = insert_match_score;
                        change_score = insert_change_score;
                    }else{
                        //deletion
                        cigar = delete_cigar;
                        score = delete_score;
                        edit = delete_edit;
                        match_score = delete_match_score;
                        change_score = delete_change_score;
                    }
                }
                if(! cigar.contains("M") && sequence.length() > 1){
                    String newCigar = "";
                    for(int i=0; i < cigar.length(); i++){
                        newCigar += "S";
                    }
                    cigar = newCigar;
                    score = 0;
                    edit = 0;
                    match_score = 0;
                    change_score = 0;
                }
            }
        }
        
        HashMap<String, String> return_map = new HashMap<>();
        return_map.put("cigar", cigar);
        return_map.put("score", "" + score);
        return_map.put("edit", "" + edit);
        return_map.put("match_score", "" + match_score);
        return_map.put("change_score", "" + change_score);
        return return_map;
    }
    
    private int getMappingQuality(int numberOfReads, MappingPosition mappingPosition, HashSet<MappingPosition> mappingPositionsSet){
        if (Mapping.MAPPING_QUALITY_TYPE.equals("simple")){
            return this.getSimpleQuality(numberOfReads);
        }else if(Mapping.MAPPING_QUALITY_TYPE.equals("bwa-like")){
            if(numberOfReads == 0){
                return 0;
            }else{
                double base_prob = 0.99;
                double totalChances = 0.0;
                for (MappingPosition mp : mappingPositionsSet){
                    totalChances += this.getBinomialProbability(mp.getMatches(), 
                            mp.getInterestingPositionsCount(), base_prob);
                }
                double cur_prob = 0;
                if (totalChances != 0){
                    cur_prob = (this.getBinomialProbability(mappingPosition.getMatches(), 
                        mappingPosition.getInterestingPositionsCount(), base_prob)) / (totalChances);
                }
                return this.getPhredScore(1 - cur_prob);
            }
        }
        return 0;
    }
    
    private int getSimpleQuality(int numberOfReads){
        int mapQ = 255;
        if (numberOfReads == 1){
            mapQ = 60;
        }else if(numberOfReads == 2){
            mapQ = 3;
        }else if(numberOfReads == 3){
            mapQ = 2;
        }else if(numberOfReads <= 9){
            mapQ = 1;
        }else{
            mapQ = 0;
        }
        return mapQ;
    }
    
    private int getPhredScore(double probility){
        if (probility >= 1){
            probility = 0.999999;
        }
        double lowProp = 0.000001;
        if (probility <= lowProp){
            probility = lowProp;
        }
        double phred = -10 * Math.log10(probility);
        return (int) Math.round(phred);
    }
    
    /**
     * implemented as found on https://lists.gnu.org/archive/html/octave-maintainers/2011-09/pdfK0uKOST642.pdf
     * @param successes
     * @param tests
     * @param chance
     * @return 
     */
    private double getBinomialProbability(int successes, int tests, double chance){
        if (2*successes > tests){
            return this.getBinomialProbability(tests - successes, tests, 1- chance);
        }
        double f = 1.0;
        int j0 = 0;
        int j1 = 0;
        int j2 = 0;
        while((j0<successes) || (j1 < successes) || (j2<tests - successes)){
            if ((j0<successes) && (f<1)){
                j0++;
                f *= (double) (tests-successes+j0)/(j0*1.0);
            }else{
                if (j1 < successes){
                    j1++;
                    f *= chance;
                }else{
                    j2++;
                    f *= 1-chance;
                }
            }
        }
        return f;
    }
    
}


