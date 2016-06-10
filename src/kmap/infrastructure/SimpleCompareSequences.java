/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap.infrastructure;

import java.util.HashMap;
import kmap.model.CompareOutput;
import kmap.model.MappingParameters;

/**
 *
 * @author koen
 */
public class SimpleCompareSequences implements CompareSequences{

    private MappingParameters parameters;
    
    public SimpleCompareSequences(MappingParameters parameters){
        this.parameters = parameters;
    }
    
    @Override
    public CompareOutput compare(String reference, String sequence, int differences) {
        //search for mismatches first
        String cigar = "";
        double score = 0.0;
        int edit = 0;
        int match_score = 0;
        int change_score = 0;
        if (sequence.length() <= reference.length() && sequence.equals(reference.substring(0, sequence.length()))){
            //complete match
            for (int i=0; i < sequence.length(); i++){
                cigar += "M";
            }
            score = this.parameters.getCorrect_base_score() * sequence.length();
            edit = 0;
            match_score = sequence.length();
            change_score = 0;
        }else if(differences < 0 || reference.length() == 0){
            //no differences allowed any more => soft clipping
            for (int i =0; i < sequence.length(); i++){
                cigar = cigar + "S";
            }
            score = 0;
            edit = 0;
            match_score = 0;
            change_score = 0;
        }else{
            //try new, first check if base is equal
            //HashMap<String, String> result_map;
            //mismatch or same
            String mismatch_cigar;
            double mismatch_score;
            int mismatch_edit;
            int mismatch_match_score;
            int mismatch_dif = 0;
            if (sequence.charAt(0) != reference.charAt(0)){
                mismatch_dif = 1;
            }
            CompareOutput further_compare_output = this.compare(reference.substring(1), sequence.substring(1), 
                    differences - mismatch_dif);
            mismatch_match_score = further_compare_output.getMatchScore() + 1;
            int mismatch_change_score = 0;
            if (sequence.charAt(0) == reference.charAt(0)){
                cigar = "M" + further_compare_output.getCigar();
                score = further_compare_output.getScore() + this.parameters.getCorrect_base_score();
                edit = further_compare_output.getEdit();
                match_score = mismatch_match_score;
                mismatch_change_score = further_compare_output.getChangeScore();
            }else{
                String base = "X";
                if (this.parameters.getCigarType().equals("traditional")){
                    base = "M";
                }
                mismatch_cigar = base + further_compare_output.getCigar();
                mismatch_score = further_compare_output.getScore()
                        + this.parameters.getMismatch_base_score();
                mismatch_edit = further_compare_output.getEdit() + 1;
                mismatch_change_score = further_compare_output.getChangeScore() + 1;
                match_score = mismatch_match_score;
                cigar = mismatch_cigar;
                score = mismatch_score;
            
                if(match_score != sequence.length()){
                    //insert
                    String insert_cigar;
                    double insert_score;
                    int insert_edit;
                    int insert_match_score;
                    int insert_change_score;
                    if (reference.length() == sequence.length()){
                        insert_cigar = "";
                        for (int i=0; i < reference.length(); i++){
                            insert_cigar += "S";
                        }
                        insert_edit = 0;
                        insert_score = 0;
                        insert_match_score = 0;
                        insert_change_score = 0;
                    }else{
                        further_compare_output = this.compare(reference, sequence.substring(1), differences -1);
                        insert_cigar = "I" + further_compare_output.getCigar();
                        insert_score = further_compare_output.getScore() 
                                + this.parameters.getIndel_score();
                        insert_edit = further_compare_output.getEdit() + 1;
                        //insert stays on reference position
                        insert_match_score = further_compare_output.getMatchScore();
                        insert_change_score = further_compare_output.getChangeScore() + 1;
                    }
                    //delete
                    String delete_cigar;
                    double delete_score;
                    int delete_edit;
                    int delete_match_score;
                    int delete_change_score;
                    if (reference.length() == sequence.length()){
                        delete_cigar = "";
                        for(int i=0; i < reference.length(); i++){
                            delete_cigar += "S";
                        }
                        delete_edit = 0;
                        delete_score = 0;
                        delete_match_score = 0;
                        delete_change_score = 0;
                    }else{
                        further_compare_output = this.compare(reference.substring(1), sequence, differences -1);
                        delete_cigar = "D" + further_compare_output.getCigar();
                        delete_score = further_compare_output.getScore() 
                                + this.parameters.getIndel_score();
                        delete_edit = further_compare_output.getEdit() + 1;
                        //delete skips reference position
                        delete_match_score = further_compare_output.getMatchScore() + 1;
                        delete_change_score = further_compare_output.getChangeScore() + 1;
                    }
                    
                    boolean isMismatch = false;
                    boolean isInsert = false;
                    boolean isDelete = false;
                    int mismatch_correct = 0;
                    int insert_correct = 0;
                    int delete_correct = 0;
                    if (mismatch_cigar.contains("D")){
                        for (char c : mismatch_cigar.toCharArray()){
                            if (c == 'D') mismatch_correct++;
                        }
                    }
                    if (insert_cigar.contains("D")){
                        for (char c : insert_cigar.toCharArray()){
                            if (c == 'D') insert_correct++;
                        }
                    }
                    if (delete_cigar.contains("D")){
                        for (char c : delete_cigar.toCharArray()){
                            if (c == 'D') delete_correct++;
                        }
                    }
                    if ((mismatch_match_score - mismatch_correct) > (insert_match_score - insert_correct)
                            && (mismatch_match_score - mismatch_correct) > (delete_match_score - delete_correct)){
                        //MISMATCH score is the highest
                        isMismatch = true;
                    }else if((insert_match_score - insert_correct) > (mismatch_match_score - mismatch_correct) 
                            && (insert_match_score - insert_correct) > (delete_match_score - delete_correct)){
                        //INSERT score is the highest
                        isInsert = true;
                    }else if((delete_match_score - delete_correct) > (mismatch_match_score - mismatch_correct) 
                            && (delete_match_score - delete_correct) > (insert_match_score - insert_correct)){
                        //DELETE score is the highest
                        isDelete = true;
                    }else if((mismatch_match_score - mismatch_correct) == (insert_match_score - insert_correct) 
                            && (mismatch_match_score - mismatch_correct) > (delete_match_score - delete_correct)){
                        //MISMATCH and INSERT equals
                        if (mismatch_edit < insert_edit){
                            //MISMATCH edit lowest
                            isMismatch = true;
                        }else if(mismatch_edit > insert_edit){
                            //INSERT edit lowest
                            isInsert = true;
                        }else{
                            //MISMATCH and INSERT edit equals => MISMATCH
                            isMismatch = true;
                        }
                    }else if((mismatch_match_score - mismatch_correct) == (delete_match_score - delete_correct) 
                            && (mismatch_match_score - mismatch_correct) > (insert_match_score - insert_correct)){
                        //MISMATCH and DELETE equals
                        if (mismatch_edit < delete_edit){
                            //MISMATCH edit lowest
                            isMismatch = true;
                        }else if(mismatch_edit > delete_edit){
                            //DELETE edit lowest
                            isDelete = true;
                        }else{
                            //MISMATCH and DELETE edit equalse => MISMATCH
                            isMismatch = true;
                        }
                    }else if((insert_match_score - insert_correct) == (delete_match_score - delete_correct) 
                            && (mismatch_match_score - mismatch_correct) > (delete_match_score - delete_correct)){
                        //INSERT and DELETE equals
                        if (insert_edit < delete_edit){
                            //INSERT edit lowest
                            isInsert = true;
                        }else if(insert_edit > delete_edit){
                            //DELETE edit lowest
                            isDelete = true;
                        }else{
                            //INSERT and DELETE edit equals => INSERT?
                            isInsert = true;
                        }
                    }else{
                        //MISMATCH and INSERT and DELETE equals
                        if (mismatch_edit <= insert_edit && mismatch_edit <= delete_edit){
                            //MISMATCH
                            isMismatch = true;
                        }else if(insert_edit < mismatch_edit && insert_edit <= delete_edit){
                            //INSERT
                            isInsert = true;
                        }else if(delete_edit < mismatch_edit && delete_edit < insert_edit){
                            //DELETE
                            isDelete = true;
                        }else{
                            //MISMATCH?
                            isMismatch = true;
                        }
                    }
                    
                    
                    if (isMismatch){
                        //mismatch edit is lower, so mismatch
                        cigar = mismatch_cigar;
                        score = mismatch_score;
                        edit = mismatch_edit;
                        match_score = mismatch_match_score;
                        change_score = mismatch_change_score;
                    }else if(isInsert){
                        //insert is lower, so insert
                        cigar = insert_cigar;
                        score = insert_score;
                        edit = insert_edit;
                        match_score = insert_match_score;
                        change_score = insert_change_score;
                    }else{
                        //deletion
                        cigar = delete_cigar;
                        score = delete_score;
                        edit = delete_edit;
                        match_score = delete_match_score;
                        change_score = delete_change_score;
                    }
                }
                if(! cigar.contains("M") && sequence.length() > 1){
                    String newCigar = "";
                    for(int i=0; i < cigar.length(); i++){
                        newCigar += "S";
                    }
                    cigar = newCigar;
                    score = 0;
                    edit = 0;
                    match_score = 0;
                    change_score = 0;
                }
            }
        }
        
        CompareOutput compareOutput = new CompareOutput(cigar, score, edit, match_score, change_score);
        return compareOutput;
    }
    
}
