/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmap.infrastructure;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import kmap.model.SamRead;

/**
 *
 * @author koen
 */
public class SamFileWriter {
        
    private final String outputDir;
    private final BufferedWriter writer;
    
    public SamFileWriter(String outputDir, String sampleName) throws IOException{
        this.outputDir = outputDir;
        this.writer = new BufferedWriter(new OutputStreamWriter(new DataOutputStream(new FileOutputStream(new File(this.outputDir + File.separator + sampleName + ".sam")))));
        
    }
    
    public void writeLine(String line) throws IOException{
        this.writer.write(line + "\n");
    }
    
    public void writeSamLine(SamRead samRead) throws IOException{
        this.writer.write(samRead.toString() + "\n");
    }
    
    public void close() throws IOException{
        writer.close();
    }
    
    
    @Override
    public void finalize() throws Throwable{
        writer.close();
        super.finalize();
    }
    
}
