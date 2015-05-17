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
public class Location{
        
    private final String chromosome;
    private final int position;

    public Location(String chromosome, int position){
        this.chromosome = chromosome;
        this.position = position;
    }

    public String getChromosome(){
        return this.chromosome;
    }

    public int getPosition(){
        return this.position;
    }
}
