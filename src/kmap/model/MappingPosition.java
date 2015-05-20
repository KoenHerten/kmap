/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap.model;

import java.util.Objects;
import kmap.infrastructure.ReadAndCigarManipulation;

/**
 *
 * @author koen
 */
public final class MappingPosition {
    private final String sequence;
    private final String cigar;
    private final String longCigar;
    private final String chromosome;
    private final int position;
    private final int edit;
    private final double score;
    private final boolean reverse;
    private int mismatches;
    private int matches;
    private int interestingPositionsCount;

    public MappingPosition(String sequence, String cigar, String chromosome, int position, int edit, double score){
        this.sequence = sequence;
        this.longCigar = cigar;
        this.cigar = ReadAndCigarManipulation.getCigar(cigar);
        this.chromosome = chromosome;
        this.position = position;
        this.edit = edit;
        this.score = score;
        this.reverse = false;
        this.calculateCigarVariables();
    }

    public MappingPosition(String sequence, String cigar, String chromosome, int position, int edit, double score, boolean reverse){
        this.sequence = sequence;
        this.longCigar = cigar;
        this.cigar = ReadAndCigarManipulation.getCigar(cigar);
        this.chromosome = chromosome;
        this.position = position;
        this.edit = edit;
        this.score = score;
        this.reverse = reverse;
        this.calculateCigarVariables();
    }

    public String getSequence(){
        return this.sequence;
    }

    public String getCigar(){
        return this.cigar;
    }
    
    public String getLongCigar(){
        return this.longCigar;
    }

    public String getChromosome(){
        return this.chromosome;
    }

    public int getPosition(){
        return this.position;
    }

    public int getEdit(){
        return this.edit;
    }

    public double getScore(){
        return this.score;
    }

    public boolean isReverse(){
        return this.reverse;
    }

    public int getMatches(){
        return this.matches;
    }
    
    public int getMisMatches(){
        return this.mismatches;
    }
    
    public int getInterestingPositionsCount(){
        return this.interestingPositionsCount;
    }
    
    public void calculateCigarVariables(){
        this.matches = 0;
        this.mismatches = 0;
        this.interestingPositionsCount = 0;;
        for (char c : this.getLongCigar().toCharArray()){
            if (c == 'M'){
                this.matches++;
                this.interestingPositionsCount++;
            }else if (c == 'X'){
                this.mismatches++;
                this.interestingPositionsCount++;
            }else if (c == 'I'){
                this.mismatches++;
                this.interestingPositionsCount++;
            }else if (c == 'D'){
                this.mismatches++;
                this.interestingPositionsCount++;
            }
        }
    }
    
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MappingPosition other = (MappingPosition) obj;
        if (!Objects.equals(this.sequence, other.sequence)) {
            return false;
        }
        if (!Objects.equals(this.cigar, other.cigar)) {
            return false;
        }
        if (!Objects.equals(this.chromosome, other.chromosome)) {
            return false;
        }
        if (this.position != other.position) {
            return false;
        }
        if (this.edit != other.edit) {
            return false;
        }
        if (this.score != other.score) {
            return false;
        }
        if (this.reverse != other.reverse){
            return false;
        }
        return true;
    }

    @Override
    public int hashCode(){
        return (int) this.chromosome.hashCode() * this.position * (int) this.score;
    }

}
