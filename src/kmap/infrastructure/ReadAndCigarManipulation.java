/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap.infrastructure;

/**
 *
 * @author koen
 */
public class ReadAndCigarManipulation {
    
    /**
     * 
     * @param sequence String | sequence
     * @return the reverse complement sequence
     */
    public static String getReverseComplement(String sequence){
        String result = "";
        for (char base : sequence.toCharArray()){
            switch (base){
                case 'a' : result = "t" + result;
                            break;
                case 'c' : result = "g" + result;
                            break;
                case 'g' : result = "c" + result;
                            break;
                case 't' : result = "a" + result;
                            break;
                default : result = "n" + result;
                            break;
            }
        }
        return result;
    }
    
    /**
     * 
     * @param quality String
     * @return the reverse quality
     */
    public static String getReverseQuality(String quality){
        String reverse = "";
        for (char c : quality.toCharArray()){
            reverse = c + reverse;
        }
        return reverse;
    }
    
    /**
     * 
     * @param cigar String
     * @return the reverse of the cigar
     */
    public static String getCigarReverse(String cigar){
        String reverseCigar = "";
        for (int i = 0; i < cigar.length(); i++){
            reverseCigar += cigar.charAt(cigar.length() - i - 1);
        }
        return reverseCigar;
    }
    
    /**
     * 
     * @param oldcigar String | the long cigar String
     * @return the normal cigar used in the sam file
     */
    public static String getCigar(String oldcigar){
        String cigar = "";
        int count = 0;
        String oldsign = "-";
        for (int i=0; i < oldcigar.length(); i++){
            String sign = oldcigar.substring(i,i+1);
            if (sign.equals(oldsign)){
                count++;
            }else{
                if(count != 0){
                    cigar += count + oldsign;
                }
                oldsign = sign;
                count = 1;
            }
        }
        cigar += count + oldsign;
        return cigar;
    }
    
}
