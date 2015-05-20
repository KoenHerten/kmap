/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import kmap.model.MappingParameters;

/**
 *
 * @author koen
 */
public class Mapper {

    public static final int BASE_INDEX = 1;
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        String input;
        //index input
        //input = "index -reference /home/koen/Downloads/chr22.part2.fa -kmer 22 -gz false -o /home/koen/Downloads";
        //mapping input
        input = "map -gz false"
                + " -fastq /home/koen/Downloads/test.fastq"
                + " -o /home/koen/Downloads"
        //        + " -reference /home/koen/Downloads/chr22.part2.fa "
                + " -index /home/koen/Downloads"
                + " -kmer 13";
        //input = "-help";
        args = input.split(" ");
        if (args.length == 0 || args[0].toLowerCase().equals("-h")
                || args[0].toLowerCase().equals("-help")){
            MappingParameters parameters = new MappingParameters("help");
            parameters.getHelp();
        }else if (args[0].toLowerCase().equals("index")){
            MappingParameters parameters = new MappingParameters("index");
            for (int i=1; i < args.length; i++){
                parameters.setParameter(args[i], args[i+1]);
                i++;
            }
            if (parameters.areRequiredParametersSet()){
                GenomeIndexing genomeIndexing = new GenomeIndexing(parameters);
                try {
                    genomeIndexing.createAndSaveIndex();
                } catch (IOException ex) {
                    Logger.getLogger(Mapper.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println("Genome Index build failed.");
                }
            }else{
                System.err.println("NOT ALL PARAMETERS ARE SET");
            }
        }else if (args[0].toLowerCase().equals("map")){
            MappingParameters parameters = new MappingParameters("map");
            for (int i=1; i < args.length; i++){
                parameters.setParameter(args[i], args[i+1]);
                i++;
            }
            if (parameters.areRequiredParametersSet()){
                try {
                    Mapping mapping;
                    mapping = new Mapping(parameters);
                    mapping.map(parameters.getFastqFile(), parameters.isZipped());
                } catch (IOException ex) {
                    Logger.getLogger(Mapper.class.getName()).log(Level.SEVERE, null, ex);
                    System.out.println("Mapping failed.");
                }
            }else{
                System.err.println("NOT ALL PARAMETERS ARE SET");
            }
        }
        
    }
    
    
    
    
    
}
