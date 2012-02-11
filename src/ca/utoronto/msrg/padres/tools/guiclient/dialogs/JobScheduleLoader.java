/*
 * Created on 2004-8-26
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package ca.utoronto.msrg.padres.tools.guiclient.dialogs;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import ca.utoronto.msrg.padres.common.message.parser.ParseException;
import ca.utoronto.msrg.padres.tools.guiclient.SwingRMIDeployer;

import javax.swing.JFileChooser;

/**
 * @author Li Guoli
 * 
 *         To change the template for this generated type comment go to
 *         Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class JobScheduleLoader extends Thread {
	String fileName = "";

	public JobScheduleLoader() {
		System.out.println("loader ...");
	}

	public void start() {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("."));

		chooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
			public boolean accept(File f) {
				String name = f.getName().toLowerCase();
				return name.endsWith(".txt") || name.endsWith(".app") || f.isDirectory();
			}

			public String getDescription() {
				// TODO Auto-generated method stub
				return null;
			}
		});

		int r = chooser.showOpenDialog(null);
		if (r == JFileChooser.APPROVE_OPTION) {
			fileName = chooser.getSelectedFile().getAbsolutePath();
		} else {
			fileName = "";
			SwingRMIDeployer.output.append("Loading canceled!" + "\n");
			return;
		}
		try {
			// FileInputStream dataFile = new FileInputStream(fileName);

			BufferedReader in = new BufferedReader(new FileReader(fileName));
			SwingRMIDeployer.output.append("file " + fileName + " is opened." + "\n");
			String line;
			try {
				while ((line = in.readLine()) != null) {
					if ((line.indexOf("p") != -1) || (line.indexOf("a") != -1)
							|| (line.indexOf("publication") != -1)) {
						try {
							SwingRMIDeployer.handleCommand(line);
							SwingRMIDeployer.output.append(line + "\n");
						} catch (ParseException e) {
							e.printStackTrace();
						}
					}

					try {
						SwingRMIDeployer.output.paintImmediately(0, 0, 800, 800);
						Thread.sleep(500);
					} catch (InterruptedException e2) {
						// TODO Auto-generated catch block
						e2.printStackTrace();
					}

				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

		} catch (FileNotFoundException ef) {
			SwingRMIDeployer.output.append("file " + fileName + " is not found!" + "\n");
		}

	}

}
