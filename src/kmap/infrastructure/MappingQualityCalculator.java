/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap.infrastructure;

import java.util.HashSet;
import kmap.Mapping;
import kmap.model.MappingPosition;

/**
 *
 * @author koen
 */
public class MappingQualityCalculator {
    
    private final String mapping_quality_type;
    
    public MappingQualityCalculator(String mapping_quality_type){
        this.mapping_quality_type = mapping_quality_type;
    }
    
    public int getMappingQuality(int numberOfReads, MappingPosition mappingPosition, HashSet<MappingPosition> mappingPositionsSet){
        if (this.mapping_quality_type.equals("simple")){
            return this.getSimpleQuality(numberOfReads);
        }else if(this.mapping_quality_type.equals("bwa-like")){
            if(numberOfReads == 0){
                return 0;
            }else{
                double base_prob = 0.99;
                double totalChances = 0.0;
                for (MappingPosition mp : mappingPositionsSet){
                    totalChances += this.getBinomialProbability(mp.getMatches(), 
                            mp.getInterestingPositionsCount(), base_prob);
                }
                double cur_prob = 0;
                if (totalChances != 0){
                    cur_prob = (this.getBinomialProbability(mappingPosition.getMatches(), 
                        mappingPosition.getInterestingPositionsCount(), base_prob)) / (totalChances);
                }
                return this.getPhredScore(1 - cur_prob);
            }
        }
        return 0;
    }
    
    private int getSimpleQuality(int numberOfReads){
        int mapQ = 255;
        if (numberOfReads == 1){
            mapQ = 60;
        }else if(numberOfReads == 2){
            mapQ = 3;
        }else if(numberOfReads == 3){
            mapQ = 2;
        }else if(numberOfReads <= 9){
            mapQ = 1;
        }else{
            mapQ = 0;
        }
        return mapQ;
    }
    
    private int getPhredScore(double probility){
        if (probility >= 1){
            probility = 0.999999;
        }
        double lowProp = 0.000001;
        if (probility <= lowProp){
            probility = lowProp;
        }
        double phred = -10 * Math.log10(probility);
        return (int) Math.round(phred);
    }
    
    /**
     * implemented as found on https://lists.gnu.org/archive/html/octave-maintainers/2011-09/pdfK0uKOST642.pdf
     * @param successes
     * @param tests
     * @param chance
     * @return 
     */
    private double getBinomialProbability(int successes, int tests, double chance){
        if (2*successes > tests){
            return this.getBinomialProbability(tests - successes, tests, 1- chance);
        }
        double f = 1.0;
        int j0 = 0;
        int j1 = 0;
        int j2 = 0;
        while((j0<successes) || (j1 < successes) || (j2<tests - successes)){
            if ((j0<successes) && (f<1)){
                j0++;
                f *= (double) (tests-successes+j0)/(j0*1.0);
            }else{
                if (j1 < successes){
                    j1++;
                    f *= chance;
                }else{
                    j2++;
                    f *= 1-chance;
                }
            }
        }
        return f;
    }
}
