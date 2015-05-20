/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap.infrastructure;

import java.util.Comparator;
import kmap.model.MappingPosition;

/**
 *
 * @author koen
 */
public class MappingPositionComparator implements Comparator<MappingPosition>{

    @Override
    public int compare(MappingPosition o1, MappingPosition o2) {
        if (o1.getScore() == o2.getScore()){
            return 0;
        }
        if (o1.getScore() > o2.getScore()){
            return 1;
        }
        //if (o1.getScore() < o2.getScore())
        return -1;
        
    }
    
}
