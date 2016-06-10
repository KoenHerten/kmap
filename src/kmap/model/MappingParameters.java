/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap.model;

import java.util.HashMap;

/**
 *
 * @author koen
 */
public class MappingParameters {
    
    private HashMap<MappingArguments, String> string_arguments;
    private HashMap<MappingArguments, Integer> integer_arguments;
    private final String tooltype;
    
    public MappingParameters(String tooltype){
        this.tooltype = tooltype;
        this.string_arguments = new HashMap<>();
        this.integer_arguments = new HashMap<>();
        this.integer_arguments.put(MappingArguments.KMER, 20);
        this.integer_arguments.put(MappingArguments.SEED_DISTANCE, 1);
        this.integer_arguments.put(MappingArguments.OVERLAP, 1000);
        this.integer_arguments.put(MappingArguments.READ_LENGTH, 10000);
        this.integer_arguments.put(MappingArguments.CORRECT_BASE_SCORE, 1);
        this.integer_arguments.put(MappingArguments.MISMATCH_BASE_SCORE, -1);
        this.integer_arguments.put(MappingArguments.INDEL_SCORE, -2);
        this.integer_arguments.put(MappingArguments.MAX_EDIT, 4);
        this.string_arguments.put(MappingArguments.OUTPUT_DIR, System.getProperty("user.dir"));
        this.string_arguments.put(MappingArguments.GZIP, "false");
        this.string_arguments.put(MappingArguments.GZIP_REFERENCE, "false");
        this.string_arguments.put(MappingArguments.MAPPING_QUALITY_TYPE, "bwa-like");
    }
    
    public void setParameter(String parameter_name, String parameter_value){
        MappingArguments mappingArguments = MappingArguments.INVALID_ARGUMENT.getArgument(parameter_name);
        if (mappingArguments.getType().toLowerCase().equals("integer")){
            this.integer_arguments.put(mappingArguments, Integer.parseInt(parameter_value));
        }else{
            this.string_arguments.put(mappingArguments, parameter_value);
        }
    }
    
    public void setParameter(MappingArguments mappingArguments, String parameter_value){
        if (mappingArguments.getType().toLowerCase().equals("integer")){
            this.integer_arguments.put(mappingArguments, Integer.parseInt(parameter_value));
        }else{
            this.string_arguments.put(mappingArguments, parameter_value);
        }
    }
    
    public void setParameter(String parameter_name, int parameter_value){
        MappingArguments mappingArguments = MappingArguments.valueOf(parameter_name);
        if (mappingArguments.getType().toLowerCase().equals("integer")){
            this.integer_arguments.put(mappingArguments, parameter_value);
        }else{
            this.string_arguments.put(mappingArguments, "" +parameter_value);
        }
    }
    
    public void setParameter(MappingArguments mappingArguments, int parameter_value){
        if (mappingArguments.getType().toLowerCase().equals("integer")){
            this.integer_arguments.put(mappingArguments, parameter_value);
        }else{
            this.string_arguments.put(mappingArguments, "" + parameter_value);
        }
    }
    
    public boolean areRequiredParametersSet(){
        if (this.tooltype.toLowerCase().equals("index")){
            //the indexing needs a reference genome and kmer
            System.out.println("" + this.string_arguments.containsKey(MappingArguments.REFERENCE));
            System.out.println("" + this.string_arguments.containsKey(MappingArguments.KMER));
            if (this.string_arguments.containsKey(MappingArguments.REFERENCE)
                    && this.string_arguments.containsKey(MappingArguments.KMER)){
                return true;
            }else{
                return false;
            }
        }else if (this.tooltype.toLowerCase().equals("map")){
            //the mapping needs a reference location
            //or a reference and kmer
            if (this.string_arguments.containsKey(MappingArguments.INDEX_LOCATION)
                    || (this.string_arguments.containsKey(MappingArguments.REFERENCE)
                    && this.integer_arguments.containsKey(MappingArguments.KMER))){
            }else{
                return false;
            }
            if (! this.string_arguments.containsKey(MappingArguments.FASTQ_FILE)){
                return false;
            }
        }
        return true;
    }
    
    public String getToolType(){
        return this.tooltype;
    }
    
    public int getKmer(){
        return this.integer_arguments.get(MappingArguments.KMER);
    }
    
    public int getSeedDistance(){
        return this.integer_arguments.get(MappingArguments.SEED_DISTANCE);
    }
    
    public int getOverlap(){
        return this.integer_arguments.get(MappingArguments.OVERLAP);
    }
    
    public int getReadLength(){
        return this.integer_arguments.get(MappingArguments.READ_LENGTH);
    }
    
    public int getCorrect_base_score(){
        return this.integer_arguments.get(MappingArguments.CORRECT_BASE_SCORE);
    }
    
    public int getMismatch_base_score(){
        return this.integer_arguments.get(MappingArguments.MISMATCH_BASE_SCORE);
    }
    
    public int getIndel_score(){
        return this.integer_arguments.get(MappingArguments.INDEL_SCORE);
    }
    
    public int getMaxEdit(){
        return this.integer_arguments.get(MappingArguments.MAX_EDIT);
    }
    
    public String getOutputDir(){
        return this.string_arguments.get(MappingArguments.OUTPUT_DIR);
    }
    
    public String getIndexLocation(){
        return this.string_arguments.get(MappingArguments.INDEX_LOCATION);
    }
    
    public boolean hasIndexLocation(){
        return this.string_arguments.containsKey(MappingArguments.INDEX_LOCATION);
    }
    
    public String getReference(){
        return this.string_arguments.get(MappingArguments.REFERENCE);
    }
    
    public String getFastqFile(){
        return this.string_arguments.get(MappingArguments.FASTQ_FILE);
    }
    
    public boolean isZipped(){
        if (this.string_arguments.get(MappingArguments.GZIP).toLowerCase().equals("true")){
            return true;
        }else{
            return false;
        }
    }
    
    public boolean isZippedReference(){
        if (this.string_arguments.get(MappingArguments.GZIP_REFERENCE).toLowerCase().equals("true")){
            return true;
        }else{
            return false;
        }
    }
    
    public String getMappingQualityType(){
        return this.string_arguments.get(MappingArguments.MAPPING_QUALITY_TYPE);
    }
    
    public void getHelp(){
            System.out.println("Usage: java -jar kmap.java [name] [options]");
            System.out.println("Index generation (not mandatory)");
            System.out.println("Name: index ");
            System.out.println("\t -f \t path of the reference fasta file");
            System.out.println("\t -kmer \t the size of the kmer to use");
            System.out.println("\t -gz \t the fasta file is gziped (true/false)");
            System.out.println("\t -o \t output directory for the index files");
            System.out.println("\t\tExample: java -jar kmap.jar index -reference reference.fa -kmer 22 -gz false -o .");
            
            System.out.println("Mapping");
            System.out.println("Name: map");
            System.out.println("\t -fastq \t the fastq file to map, the output file will have the same name, exept .fastq is changed to .bam");
            System.out.println("\t -gz \t the fastq file is gziped (true/false)");
            System.out.println("\t -o \t the output directory");
            System.out.println("\t -kmer \t the used kmer for the index generation "
                    + "(only for use with -reference)");
            System.out.println("\t -reference \t the path to the reference fasta file "
                    + "(index is generated before mapping, index is not writen to a drive)"
                    + "(-kmer option is needed, -index option is denied)");
            System.out.println("\t -index \t the path to the index files"
                    + "(-kmer option is denied, -reference is not needed)");
            System.out.println("\t\tExample: java -jar kmap.jar map -gz false -fastq test.fastq -o . -index .");
            System.out.println("\t\tExample: java -jar kmap.jar map -gz false -fastq test.fastq -o . -reference chr22.part2.fa -kmer 15");
    }
    
}
