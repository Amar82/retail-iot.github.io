package org.eclipse.kura.example.hello_osgi;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoFrame extends Canvas {
    /** 
     * 
     */
    private static final long serialVersionUID = 3156965697737887068L;
    private static final Logger s_logger = LoggerFactory.getLogger(DemoFrame.class);
    
    public DemoFrame () {
        setBackground (Color.GRAY);
        setSize(300, 300);
     }   
    
    @Override
    public void paint(Graphics g) {
        Graphics2D g2; 
       g2 = (Graphics2D) g;
       g2.drawString ("It is a custom canvas area", 70, 70);
   }   
}