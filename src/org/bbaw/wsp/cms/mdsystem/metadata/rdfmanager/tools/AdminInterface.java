package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMain;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMainForAI;

public class AdminInterface extends JFrame {

	/**
	 * Marco Seidler (shk2) tool for creation of PNGs based to given RDFs
	 */
	private static final long serialVersionUID = 5952164735517923589L;

	public static void main(String[] args) {

		new AdminInterface();

	}

	private JButton btn_src;
	private JButton btn_go;
	private Label src;

	private JButton btn_des;
	private Label des;
	private String srcD;
	private String desF;
	private JCheckBox folder;
	private TextArea textArea;

	/**
	 * Opens a new Frame
	 */
	public AdminInterface() {
		super("Admin Interface");
		setSize(600, 300);
		setLocation(300, 300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		initalisation();
	}

	/**
	 * Creates the Elements of GUI add value change listener on slider
	 */
	private void initalisation() {
		setLayout(new BorderLayout(5, 5));

		Panel panel = new Panel();
		panel.setLayout(new GridLayout(7, 1));
		btn_src = new JButton("Choose Source Data");
		src = new Label("no Source Data selected");
		btn_des = new JButton("Choose Destination Folder");
		des = new Label("no Destination Folder selected");
		btn_go = new JButton("Start");

		addButtonListener(btn_src);
		addButtonListener(btn_go);
		addButtonListener(btn_des);

		panel.add(folder = new JCheckBox("Choose a source Folder"));

		panel.add(btn_src);
		panel.add(src);
		panel.add(btn_des);
		panel.add(des);
		panel.add(btn_go);

		textArea = new TextArea("Welcome\n");

		Panel mainPanel = new Panel();
		mainPanel.setLayout(new GridLayout(1, 2));
		mainPanel.add(panel);
		mainPanel.add(new Panel().add(textArea));

		getContentPane().add(mainPanel);

	}

	/**
	 * buttonlistener for buttons
	 * 
	 * @param b
	 */
	private void addButtonListener(JButton b) {
		b.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ev) {
				if (ev.getActionCommand().equals("Choose Source Data")) {
					if (folder.isSelected())
						srcD = fileChooser(false);
					else
						srcD = fileChooser(true);
					if (srcD == null) {
						src.setText("no Source Folder selected");
					} else
						src.setText(srcD);

				} else if (ev.getActionCommand().equals(
						"Choose Destination Folder")) {

					desF = fileChooser(false);
					if (desF == null) {
						des.setText("no Destination Folder selected");
					} else
						des.setText(desF);

				} else if (ev.getActionCommand().equals("Start")) {

					if (srcD == null) {
						println("no source choosed!");
						return;
					}
					if (desF == null) {
						println("no destination choosed!");
						return;
					}

					// ArrayList<File> liste = new ArrayList<File>();
					// liste = scanforRDF(srcF);
					// if (liste.isEmpty()) {
					// println("There are no RDF files in directory");
					// return;
					// }
					// if (liste.size() == 1) {
					// println("There is one RDF element.");
					// } else
					// println("There are " + liste.size() + " RDF elements.");
					// println("Started!");
					// execution(liste);
					println("Choosen Data: " + srcD);
					println("Choosen Destination" + desF);

					try {
						execution();
						println("finished!");

					} catch (Exception e) {
						println(e.getMessage());
					}

				}

			}
		});
	}

	private void execution() {
		new JenaMainForAI(srcD, desF).initStore();
	}

	// /**
	// * Main function calls teststore and bash
	// *
	// * @param col
	// */
	// private void execution(ArrayList<File> col) {
	//
	// String dot;
	// println("Choosed accuracy = " + slider.getValue());
	//
	// for (File file : col) {
	// try {
	//
	// dot = jenatester.testStore(file, desF, slider.getValue());
	//
	// String cmd = "dot -Tpng " + dot + " -o "
	// + dot.substring(0, dot.length() - 4) + ".png";
	// // String cmd = "dot -Tpng Briefe2.dot -o "
	// // + dot.substring(0, dot.length() - 4) + ".png";
	// bash(cmd, new File(desF));
	//
	// println("Graph created for " + file.getName());
	//
	// } catch (Exception e) {
	// println("Error occurred, continues..");
	//
	// }
	// }
	//
	// }

	/**
	 * Seperates the files with ending RDF
	 * 
	 * @param str
	 * @return
	 */
	private ArrayList<File> scanforRDF(String str) {

		File dir = new File(str);
		ArrayList<File> liste = new ArrayList<File>();
		for (File f : dir.listFiles()) {
			if (f.getName().toLowerCase().endsWith(".rdf"))
				liste.add(f);
			println(f.getName());

		}

		return liste;
	}

	/**
	 * Adds a line to the TextArea
	 * 
	 * @param str
	 */
	private void println(String str) {
		textArea.append(str + "\n");
	}

	/**
	 * Is used to Choose the folder and returns the folder path
	 * 
	 * @return
	 */
	private String fileChooser(boolean singeldata) {
		JFileChooser chooser = new JFileChooser();
		// Note: source for ExampleFileFilter can be found in
		if (!singeldata)
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		int returnVal = chooser.showOpenDialog(chooser);

		if (returnVal == JFileChooser.APPROVE_OPTION) {

			return chooser.getSelectedFile().getAbsolutePath();
		}
		return null;
	}

	/**
	 * Executes Bash jobs
	 * 
	 * @param cmd
	 * @param cwd
	 */
	private void bash(String cmd, File cwd) {
		if (System.getProperty("os.name").startsWith("Windows")) {
			println("only works with Unix-shell!");
			return;
		}
		try {
			println("Path: " + cwd.getAbsolutePath());
			println(cmd);
			ProcessBuilder pb = new ProcessBuilder("bash", "-c", cmd);
			if (cwd != null) {
				pb.directory(cwd);
			}
			pb.redirectErrorStream(true);
			Process pr = pb.start();
			BufferedReader input = new BufferedReader(new InputStreamReader(
					pr.getInputStream()));

			String line = null;

			while ((line = input.readLine()) != null) {
				println(line);
			}

			int exitVal = pr.waitFor();
			if (exitVal != 0) {
				throw new Error("Failure while executing bash command '" + cmd
						+ "'. Return code = " + exitVal);
			}
		} catch (Exception e) {
			throw new Error("Could not execute bash command '" + cmd + "'.", e);
		}
	}
}
