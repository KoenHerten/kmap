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
public class InterestingLocation extends Location{
    private int end;
    private int seed_end;
    private final int seed_start;
    private final boolean reverse;

    public InterestingLocation(String chromosome, int position, int end, int seed_start){
        super(chromosome, position);
        this.end = end;
        this.seed_end = 0;
        this.seed_start = seed_start;
        this.reverse = false;
    }

    public InterestingLocation(String chromosome, int position, int end, int seed_start, boolean reverse){
        super(chromosome, position);
        this.end = end;
        this.seed_end = 0;
        this.seed_start = seed_start;
        this.reverse = reverse;
    }

    public int getEnd(){
        return this.end;
    }

    public int getSeedEnd(){
        return this.seed_end;
    }

    public int getSeedStart(){
        return this.seed_start;
    }

    public void changeEnd(int end, int seed_end){
        this.end = end;
        this.seed_end = seed_end;
    }
}
