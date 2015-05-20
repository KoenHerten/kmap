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
public class CompareOutput {
    
    private final String cigar;
    private final double score;
    private final int edit;
    private final int match_score;
    private final int change_score;
    
    public CompareOutput(String cigar, double score, int edit, int match_score, int change_score){
        this.cigar = cigar;
        this.score = score;
        this.edit = edit;
        this.match_score = match_score;
        this.change_score = change_score;
    }
    
    public String getCigar(){
        return this.cigar;
    }
    
    public double getScore(){
        return this.score;
    }
    
    public int getEdit(){
        return this.edit;
    }
    
    public int getMatchScore(){
        return this.match_score;
    }
    
    public int getChangeScore(){
        return this.change_score;
    }
    
    
}
