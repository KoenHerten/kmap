/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap.infrastructure;

import kmap.model.CompareOutput;

/**
 *
 * @author koen
 */
public interface CompareSequences {
    
    public CompareOutput compare(String reference, String sequence, int differences);
    
}
