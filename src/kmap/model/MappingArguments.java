/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap.model;

/**
 *
 * @author koen
 */
public enum MappingArguments {
    
    /**
     * The kmer size
     */
    KMER ("-kmer", "int"),
    /**
     * The seed distance in the read (can be high for low error rates, and low snp/indel rates)
     */
    SEED_DISTANCE ("-s", "int"),
    /**
     * Overlap between reference part (must be higher then the read length)
     */
    OVERLAP ("-ov", "int"),
    /**
     * Reference Part length
     */
    READ_LENGTH ("-l", "int"),
    /**
     * Correct base score
     */
    CORRECT_BASE_SCORE ("-cs", "int"),
    /**
     * Mismatch base score
     */
    MISMATCH_BASE_SCORE ("-ms", "int"),
    /**
     * Indel base score
     */
    INDEL_SCORE ("-ids", "int"),
    /**
     * Maximum edit in a part (when extending a read)
     */
    MAX_EDIT ("-me", "int"),
    /**
     * Output directory
     */
    OUTPUT_DIR ("-o", "string"),
    /**
     * Index Location
     */
    INDEX_LOCATION ("-index", "string"),
    /**
     * Fasta/Fastq file is gziped
     */
    GZIP ("-gz", "bool"),
    /**
     * Reference file is gziped
     */
    GZIP_REFERENCE ("-gzRef", "bool"),
    /**
     * Reference fasta
     */
    REFERENCE ("-reference", "string"),
    /**
     * The first fastq file
     */
    FASTQ_FILE ("-fastq", "string"),
    /**
     * The type of calculation for the mapping quality that must be used
     */
    MAPPING_QUALITY_TYPE("-mapq", "string"),
    /**
     * the type of cigar string
     */
    CIGAR_TYPE("-cigar", "string"),
    /**
     * If the given argument was invalid
     */
    INVALID_ARGUMENT ("ERROR", "null");
    
    /**
     * returns the argument for the given name (-name), or invalid if no argument exists for the given name
     * @param sortName String | the name of the argument
     * @return MappingArguments if any exists for the given name, else MappingArguments.INVALID_ARGUMENT
     */
    public MappingArguments getArgument(String sortName){
        for (MappingArguments arg : MappingArguments.values()){
            if (sortName.equals(arg.getSortName())){
                return arg;
            }
        }
        return MappingArguments.INVALID_ARGUMENT;
    }
    
    /**
     * the name of the argument (-name)
     */
    private final String sortname; 
    private final String type;
    
    /**
     * 
     * @param sortname String | name of the argument
     */
    private MappingArguments(String sortname, String type){
        this.sortname = sortname;
        this.type = type;
    }
    
    /**
     * 
     * @return String | the option name of the argument
     */
    public String getSortName(){
        return this.sortname;
    }
    
    /**
     * 
     * @return String | the type of the variable
     */
    public String getType(){
        return this.type;
    }
}
