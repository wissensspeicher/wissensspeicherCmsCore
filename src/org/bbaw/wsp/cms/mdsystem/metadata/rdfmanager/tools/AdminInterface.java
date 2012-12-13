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
import java.io.IOException;
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
	private JCheckBox folderScr;
	private JCheckBox createDataset;
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
		btn_des = new JButton("Choose Destination");
		des = new Label("no Destination selected");
		btn_go = new JButton("Start");

		addButtonListener(btn_src);
		addButtonListener(btn_go);
		addButtonListener(btn_des);

		panel.add(folderScr = new JCheckBox("Choose a source Folder"));
		panel.add(createDataset = new JCheckBox("Create a new Dataset"));
		panel.add(btn_src);
		panel.add(src);
		panel.add(btn_des);
		panel.add(des);
		panel.add(btn_go);

		textArea = new TextArea("Welcome,\n");

		Panel mainPanel = new Panel();
		mainPanel.setLayout(new GridLayout(1, 2));
		mainPanel.add(panel);
		mainPanel.add(new Panel().add(textArea));

		getContentPane().add(mainPanel);
		giveInstruct();
	}

	private void giveInstruct() {
		println("Short instruction about main features: \n" + "\n"
				+ "The two Checkboxes:\n"
				+ "1. is used to choose a whole folder \n "
				+ "where all possible data will be selected \n"
				+ "2. is used to create a new dataset, \n"
				+ "to do so there will open a save \n"
				+ "dialog in \"Choose Destination\" option\n\n"
				+ "NEVER SAVE A NEW ONE IN A FOLDER \n"
				+ "WHERE ALREADY A \".store\" EXISTS! \n\n"
				+ "if none of the Checkboxes is selected \n"

				+ "the default is:\n"
				+ "choose 1 file to add to an existing dataset." + "\n"
				+ "Good luck" + "");
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
					if (folderScr.isSelected())
						srcD = fileChooser(false, false);
					else
						srcD = fileChooser(true, false);
					if (srcD == null) {
						src.setText("no Source Folder selected");
					} else
						src.setText(srcD);

				} else if (ev.getActionCommand().equals("Choose Destination")) {

					desF = fileChooser(true, true);
					if (desF == null) {
						des.setText("no Destination selected");
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

	private void execution() throws ClassNotFoundException, IOException {
		new JenaMainForAI(srcD, desF).initStore(createDataset.isSelected());
	}

	/**
	 * Seperates the files with ending RDF
	 * 
	 * @param str
	 * @return
	 */
	@SuppressWarnings("unused")
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
	private String fileChooser(boolean singeldata, boolean aboutSet) {
		JFileChooser chooser = new JFileChooser();
		int returnVal = 0;
		Boolean save = false;
		// Note: source for ExampleFileFilter can be found in
		if (!singeldata) {
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		}

		if (aboutSet) {
			chooser.setFileFilter(new DataFilterStore());
		} else
			chooser.setFileFilter(new DataFilterFile());

		if (createDataset.isSelected() && aboutSet) {
			returnVal = chooser.showSaveDialog(chooser);
			save = true;
		} else {
			returnVal = chooser.showOpenDialog(chooser);
			save = false;
		}
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			String file = chooser.getSelectedFile().getAbsolutePath();

			if (save) {

				if (!file.toLowerCase().endsWith(".store")) {
					file += ".store";
				}
			}
			return file;

		}
		return null;
	}

	/**
	 * Creates the Filefilter for filechooser, in this case it filters for .nt
	 * .ttl .rdf types
	 * 
	 * @author shk2
	 * 
	 */
	class DataFilterFile extends javax.swing.filechooser.FileFilter {
		public String getDescription() {
			return ".nt, .ttl, .rdf";
		}

		public boolean accept(File file) {
			if (file.isDirectory())
				return true;
			else if (file.getName().toLowerCase().endsWith(".nt"))
				return true;
			else if (file.getName().toLowerCase().endsWith(".ttl"))
				return true;
			else if (file.getName().toLowerCase().endsWith(".rdf"))
				return true;
			else
				return false;
		}
	}

	/**
	 * Creates the Filefilter for filechooser, in this case it filters for
	 * .store types
	 * 
	 * @author shk2
	 * 
	 */
	class DataFilterStore extends javax.swing.filechooser.FileFilter {
		public String getDescription() {
			return ".store";
		}

		public boolean accept(File file) {
			if (file.isDirectory())
				return true;
			else if (file.getName().toLowerCase().endsWith(".store"))
				return true;

			else
				return false;
		}
	}

	/**
	 * Executes Bash jobs
	 * 
	 * @param cmd
	 * @param cwd
	 */
	@SuppressWarnings("unused")
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
