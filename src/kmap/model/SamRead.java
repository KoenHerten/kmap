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
public class SamRead {
    
    private final String q_name;
    private final int flag;
    private final String ref_name;
    private final int position;
    private final int mapQ;
    private final String cigar;
    private final String rnext;
    private final int pnext;
    private final int tlen;
    private final String seq;
    private final String qual;
    private String extra_fields;
    
    public SamRead(String q_name, int flag, String ref_name, int position, 
            int mapQ, String cigar, String rnext, int pnext, int tlen, 
            String seq, String qual){
        this.q_name = q_name;
        this.flag = flag;
        this.ref_name = ref_name;
        this.position = position;
        this.mapQ = mapQ;
        this.cigar = cigar;
        this.rnext = rnext;
        this.pnext = pnext;
        this.tlen = tlen;
        this.seq = seq;
        this.qual = qual;
        this.extra_fields = null;
    }
    
    public SamRead(String q_name, int flag, String ref_name, int position, 
            int mapQ, String cigar, String rnext, int pnext, int tlen, 
            String seq, String qual, String extra_fields){
        this.q_name = q_name;
        this.flag = flag;
        this.ref_name = ref_name;
        this.position = position;
        this.mapQ = mapQ;
        this.cigar = cigar;
        this.rnext = rnext;
        this.pnext = pnext;
        this.tlen = tlen;
        this.seq = seq;
        this.qual = qual;
        this.extra_fields = extra_fields;
    }
    
    public String getQname(){
        return this.q_name;
    }
    
    public int getFlag(){
        return this.flag;
    }
    
    public String getRefName(){
        return this.ref_name;
    }
    
    public int getPosition(){
        return this.position;
    }
    
    public int getMapQ(){
        return this.mapQ;
    }
    
    public String getCigar(){
        return this.cigar;
    }
    
    public String getRnext(){
        return this.rnext;
    }
    
    public int getPnext(){
        return this.pnext;
    }
    
    public int getTlen(){
        return this.tlen;
    }
    
    public String getSequence(){
        return this.seq;
    }
    
    public String getQual(){
        return this.qual;
    }
    
    public String getExtraFields(){
        return this.extra_fields;
    }
    
    @Override
    public String toString(){
        String extra = "";
        if (this.extra_fields != null){
            extra = "\t" + this.extra_fields;
        }
        return this.getQname() + "\t" + this.getFlag() + "\t" + this.getRefName()
                + "\t" + this.getPosition() + "\t" + this.getMapQ() + "\t" + this.getCigar()
                + "\t" + this.getRnext() + "\t" + this.getPnext() + "\t" + this.getTlen()
                + "\t" + this.getSequence() + "\t" + this.getQual() + extra;
    }
    
}
