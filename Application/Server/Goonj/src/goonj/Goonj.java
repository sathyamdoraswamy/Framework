/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package goonj;

import com.frameworkserver.Framework;

/**
 *
 * @author sathyam
 */
public class Goonj {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {        
        Framework fw = new Framework(8010, "/home/sathyam/Application", "/home/sathyam/Application");
        fw.start();
    }
}
