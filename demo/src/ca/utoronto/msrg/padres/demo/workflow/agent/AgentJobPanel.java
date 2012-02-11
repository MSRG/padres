//=============================================================================
//This file is part of The PADRES Project.
//
//For more information, see http://www.msrg.utoronto.ca
//
//Copyright (c) 2003 Middleware Systems Research Group, University of Toronto
//=============================================================================
//$Id$
//=============================================================================
package ca.utoronto.msrg.padres.demo.workflow.agent;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import java.awt.Color;
import java.awt.BorderLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 * @author Pengcheng Wan
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class AgentJobPanel extends JPanel implements ActionListener{

    JButton startButton;
    JButton exitButton;
    JTextField statusField;
    /**
     * 
     */
    public AgentJobPanel(String agentName) {
        super();
        init(agentName);
    }

    public void init(String agentName){
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        this.add(Box.createRigidArea(new Dimension(1, 25))); 

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.X_AXIS));
        JLabel label = new JLabel("Job Status:");
     	statusField = new JTextField("");
     	statusField.setBorder(BorderFactory.createLineBorder(Color.black));
     	statusField.setEditable(false);
     	statusPanel.add(Box.createRigidArea(new Dimension(5, 1)));
        statusPanel.add(label);
     	statusPanel.add(Box.createRigidArea(new Dimension(5, 1)));
        statusPanel.add(statusField);
     	statusPanel.add(Box.createRigidArea(new Dimension(5, 1)));
        this.add(statusPanel);
        
        this.add(Box.createRigidArea(new Dimension(1, 25))); 

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));
     	
        startButton = new JButton("Success");
        exitButton = new JButton("Exit");
        startButton.addActionListener(this);
        exitButton.addActionListener(this);

        buttonPanel.add(Box.createRigidArea(new Dimension(20,1)));
        buttonPanel.add(startButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(20,1)));
        buttonPanel.add(exitButton);
        this.add(buttonPanel);
        
        this.add(Box.createRigidArea(new Dimension(1, 15))); 
        
        // Build a JFrame to hold this panel
        WindowListener l = new WindowAdapter() {
            public void windowClosing(WindowEvent e) {System.exit(0);}
        };
        JFrame f = new JFrame("Padres Agent Name: " + agentName);
        //f.addWindowListener(l);
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.getContentPane().add(this, BorderLayout.CENTER);
        f.pack();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int w = 400;
        int h = 150;
        f.setLocation(screenSize.width/2 - w/2, screenSize.height/2 - h/2);
        f.setSize(w, h);
        f.setVisible(true);
    }
    
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == startButton) {
            //startButton.setEnabled(false);
            //exitButton.setEnabled(true);
        } else if (e.getSource() == exitButton) {
            int n = JOptionPane.showConfirmDialog(this,
                    "This will exit from Agent. Are you sure?",
                    "Exit confirmation",
                    JOptionPane.YES_NO_OPTION);
            if (n == JOptionPane.YES_OPTION) {
                System.exit(0);
            } else if (n == JOptionPane.NO_OPTION) {
            } else {
            }
        }
    }

    public void setStatus(String status){
        statusField.setEditable(true);
        statusField.setText(status);
        statusField.setEditable(false);
    }
    
    public void setButton(String text){
        startButton.setText(text);
        //startButton.setEnabled(false);
    }
    
    
    public static void main(String[] args) {
        AgentJobPanel ajp = new AgentJobPanel("agentA");
        ajp.setStatus("Now the agent is running....");
        ajp.setButton("Running...");
        try{
            Thread.sleep(20*1000);
            ajp.setStatus("the agent job is finished!");
            ajp.setButton("Success");
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
