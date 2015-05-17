/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

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
        //String input;
        //index input
        //input = "index -f /home/koen/Downloads/chr22.part2.fa -kmer 22 -gz false -o /home/koen/Downloads";
        //mapping input
        //input = "map -gz false "
        //        + " -fastq /home/koen/Downloads/test.fastq"
        //        + " -o /home/koen/Downloads"
        //        + " -reference /home/koen/Downloads/chr22.part2.fa "
        //        + " -index /home/koen/Downloads"
        //        + " -kmer 13";
        //input = "-help";
        //args = input.split(" ");
        if (args.length == 0 || args[0].toLowerCase().equals("-h")
                || args[0].toLowerCase().equals("-help")){
            System.out.println("Usage: java -jar kmap.java [name] [options]");
            System.out.println("Index generation (not mandatory)");
            System.out.println("Name: index ");
            System.out.println("\t -f \t path of the reference fasta file");
            System.out.println("\t -kmer \t the size of the kmer to use");
            System.out.println("\t -gz \t the fasta file is gziped (true/false)");
            System.out.println("\t -o \t output directory for the index files");
            
            System.out.println("Mapping");
            System.out.println("Name: map");
            System.out.println("\t -fastq \t the fastq file to map");
            System.out.println("\t -gz \t the fastq file is gziped (true/false)");
            System.out.println("\t -o \t the output directory");
            System.out.println("\t -kmer \t the used kmer for the index generation "
                    + "(only for use with -reference)");
            System.out.println("\t -reference \t the path to the reference fasta file "
                    + "(index is generated before mapping, index is not writen to a drive)"
                    + "(-kmer option is needed, -index option is denied)");
            System.out.println("\t -index \t the path to the index files"
                    + "(-kmer option is denied, -reference is not needed)");
        }else if (args[0].toLowerCase().equals("index")){
            String fastaFile = "";
            int kmer = 20;
            boolean isZippedFasta = false;
            String outputDir = "";
            for (int i=1; i < args.length; i++){
                if (args[i].toLowerCase().equals("-f")){
                    fastaFile = args[i+1];
                    i++;
                    System.out.println("File found: " + fastaFile);
                }else if (args[i].toLowerCase().equals("-kmer")){
                    kmer = Integer.parseInt(args[i+1]);
                    i++;
                    System.out.println("Kmer found: " + kmer);
                }else if(args[i].toLowerCase().equals("-gz")){
                    if (args[i+1].toLowerCase().equals("true")){
                        isZippedFasta = true;
                    }else{
                        isZippedFasta = false;
                    }
                    i++;
                    System.out.println("Gzip found: " + isZippedFasta);
                }else if(args[i].toLowerCase().equals("-o")){
                    outputDir = args[i+1];
                    i++;
                }
            }
            GenomeIndexing genomeIndexing = new GenomeIndexing(fastaFile, kmer, isZippedFasta, outputDir);
            try {
                genomeIndexing.createAndSaveIndex();
            } catch (IOException ex) {
                Logger.getLogger(Mapper.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Genome Index build failed.");
            }
        }else if (args[0].toLowerCase().equals("map")){
            String indexFile = null;
            boolean isZippedFastq = false;
            String outputDir = "" + System.getProperty("user.dir");
            String inputFastq = "";
            String fastaFile = "";
            int kmer = 22;
            for (int i=1; i < args.length; i++){
                
                if (args[i].toLowerCase().equals("-reference")){
                    fastaFile = args[i+1];
                    i++;
                    System.out.println("Reference found: " + fastaFile);
                }else if (args[i].toLowerCase().equals("-kmer")){
                    kmer = Integer.parseInt(args[i+1]);
                    i++;
                    System.out.println("Kmer found: " + kmer);
                }else if (args[i].toLowerCase().equals("-index")){
                    indexFile = args[i+1];
                    i++;
                    System.out.println("Index found: " + indexFile);
                }else if(args[i].toLowerCase().equals("-gz")){
                    if (args[i+1].toLowerCase().equals("true")){
                        isZippedFastq = true;
                    }else{
                        isZippedFastq = false;
                    }
                    i++;
                    System.out.println("Gzip found: " + isZippedFastq);
                }else if(args[i].toLowerCase().equals("-o")){
                    outputDir = args[i+1];
                    i++;
                    System.out.println("Output directory found: " + outputDir);
                }else if(args[i].toLowerCase().equals("-fastq")){
                    inputFastq = args[i+1];
                    i++;
                    System.out.println("Fastq file found: " + inputFastq);
                }
            }
            try {
                Mapping mapping;
                if (indexFile != null){
                    System.out.println("" + indexFile);
                    mapping = new Mapping(indexFile, outputDir);
                }else{
                    mapping = new Mapping(fastaFile, kmer, outputDir);
                }
                mapping.map(inputFastq, isZippedFastq);
            } catch (IOException ex) {
                Logger.getLogger(Mapper.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Genome Index build failed.");
            }
        }
        
    }
    
    
    
    
    
}
