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
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMainForAI;

public class AdminInterface extends JFrame {

	/**
	 * Marco Seidler (shk2) tool for creation or appending a
	 * Dataset/Triplestore, with one or more rdf ttl nt file at the same time,
	 * also available is checking of already existing models and removing of
	 * models
	 */
	private static final long serialVersionUID = 5952164735517923589L;

	public static void main(String[] args) {

		new AdminInterface();

	}

	private JButton btn_src;
	private JButton btn_go;
	private Label src;

	private JButton btn_des;
	private JButton load_set_btn;
	private JButton remove_btn;
	private Label des;
	private String srcD;
	private String desF;
	private JCheckBox folderScr;
	private JCheckBox createDataset;
	private TextArea textArea;
	@SuppressWarnings("unused")
	private boolean createNewSet;
	@SuppressWarnings("rawtypes")
	private final JComboBox combobox = new JComboBox();
	private String namedGraph = "";
	private final JenaMainForAI jenaMain = new JenaMainForAI();

	// ButtonsNames for distinction
	private static final String SOURCE_BUTTON = "Choose Metadata";
	private static final String DESTINATION_BUTTON = "Choose Destination";
	private static final String START_BUTTON = "Add to Triplestore";
	private static final String LOAD_NAMEDMODELLS_BUTTON = "Load Models";
	private static final String REMOVE_BUTTON = "Remove";

	/**
	 * Opens a new Frame
	 */
	public AdminInterface() {
		super("Admin Interface");
		setSize(900, 300);
		setLocation(300, 300);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setVisible(true);
		initalisation();
	}

	/**
	 * Creates the Elements of GUI, adds Buttons with Buttonlistener and Labels,
	 * also adds value change listener on slider
	 */
	private void initalisation() {
		setLayout(new BorderLayout(5, 5));

		Panel panel = new Panel();
		panel.setLayout(new GridLayout(7, 1));
		btn_src = new JButton(SOURCE_BUTTON);
		src = new Label("no Source Data selected");
		btn_des = new JButton(DESTINATION_BUTTON);
		des = new Label("no Destination selected");
		btn_go = new JButton(START_BUTTON);
		load_set_btn = new JButton(LOAD_NAMEDMODELLS_BUTTON);
		remove_btn = new JButton(REMOVE_BUTTON);
		combobox.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				namedGraph = (String) combobox.getSelectedItem();

			}
		});

		addButtonListener(btn_src);
		addButtonListener(btn_go);
		addButtonListener(btn_des);
		addButtonListener(remove_btn);
		addButtonListener(load_set_btn);

		panel.add(folderScr = new JCheckBox("Choose a source Folder"));
		panel.add(createDataset = new JCheckBox("Create a Dataset new Location"));
		panel.add(btn_src);
		panel.add(src);
		panel.add(btn_des);
		panel.add(des);
		panel.add(btn_go);

		textArea = new TextArea("Welcome,\n");

		Panel removePanel = new Panel();
		removePanel.setLayout(new GridLayout(7, 1));
		removePanel.add(new Label("Remove Area"));
		removePanel.add(new Label("Load all Named Models from destination"));
		removePanel.add(load_set_btn);
		removePanel.add(combobox);
		removePanel.add(new Label(""));
		removePanel.add(new Label(""));
		removePanel.add(remove_btn);

		Panel mainPanel = new Panel();
		mainPanel.setLayout(new GridLayout(1, 3));
		mainPanel.add(panel);
		mainPanel.add(new Panel().add(textArea));
		mainPanel.add(removePanel);

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
				+ "If none of the Checkboxes is selected \n"
				+ "the default is:\n"
				+ "choose 1 file to add to an existing dataset." + "\n\n"
				+ "Load Models lists all NamedModel which are\n"
				+ "currently in the choosen Dataset.\n\n"
				+ "Remove deletes the choosen NamedModel\nfrom Dataset."
				+ "\n\nIf there already is a NamedModel with equal\nname,"
				+ "it will be replaced by the newer version");
	}

	/**
	 * buttonlistener executes all the main Events differenced by which Button
	 * where pressed. Also exceptionhandling when wrong data are given
	 * 
	 * @param b
	 */
	private void addButtonListener(JButton b) {
		b.addActionListener(new ActionListener() {

			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent ev) {
				if (ev.getActionCommand().equals(SOURCE_BUTTON)) {
					if (folderScr.isSelected()) {
						srcD = fileChooser(false, false);
						scanforRDF(srcD);
					} else
						srcD = fileChooser(true, false);
					if (srcD == null) {
						src.setText("no Source Folder selected");
					} else
						src.setText(srcD);

				} else if (ev.getActionCommand().equals(DESTINATION_BUTTON)) {

					desF = fileChooser(true, true);
					if (desF == null) {
						des.setText("no Destination selected");
					} else
						des.setText(desF);

				} else if (ev.getActionCommand().equals(
						LOAD_NAMEDMODELLS_BUTTON)) {
					if (desF == null) {
						println("no source choosed!");
						return;
					}
					jenaMain.setDestination(desF);
					ArrayList<String> models = jenaMain.getModels();
					if (models.isEmpty()) {
						println("There are no models in the choosen Dataset\n"
								+ desF);
						return;
					}
					for (String s : models) {
						if (checkAlreadyinList(s))
							combobox.addItem(s);

					}
					combobox.showPopup();

					println("Loaded all named Graphes from " + desF);
					namedGraph = (String) combobox.getItemAt(0);

				} else if (ev.getActionCommand().equals(REMOVE_BUTTON)) {
					if (desF == null) {
						println("no source choosed!");
						return;
					} else if (combobox.getItemCount() == 0) {
						println("No Models available");
						return;
					}
					// JenaMainForAI jMain = new JenaMainForAI(" ", desF);
					jenaMain.setDestination(desF);
					jenaMain.removeModel(namedGraph);
					println(namedGraph + " successfully removed.");
					println("Index will refresh now.");
					combobox.removeAllItems();
					for (String s : jenaMain.getModels()) {
						combobox.addItem(s);
					}

				}

				else if (ev.getActionCommand().equals(START_BUTTON)) {

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
						errorMessage(e.getMessage());
					}

				}

			}
		});
	}

	/**
	 * Checks if there already is a Item with the given name in the Checkbox, if
	 * thats the case return = false;
	 * 
	 * @param name
	 * @return
	 */
	public boolean checkAlreadyinList(String name) {

		for (int i = 0; i < combobox.getItemCount(); ++i) {
			String temp = (String) combobox.getItemAt(i);
			if (temp.equals(name))
				return false;
		}

		return true;
	}

	/**
	 * Calls the JenaMain gives params source destination and the information if
	 * new Set has to be created
	 * 
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private void execution() throws ClassNotFoundException, IOException {

		jenaMain.setSource(srcD);
		jenaMain.setDestination(desF);
		jenaMain.initStore(createDataset.isSelected());

		createNewSet = false;
		createDataset.setSelected(false);
	}

	/**
	 * Separates the files with ending rdf,ttl,nt returns a list of valid items
	 * 
	 * @param str
	 * @return
	 */

	private ArrayList<File> scanforRDF(String str) {

		File dir = new File(str);
		ArrayList<File> liste = new ArrayList<File>();
		println("\nValid Files in the Folder:\n");
		for (File f : dir.listFiles()) {
			if (f.getName().toLowerCase().endsWith(".rdf")
					|| f.getName().toLowerCase().endsWith(".ttl")
					|| f.getName().toLowerCase().endsWith(".nt"))
				liste.add(f);
			println(" - " + f.getName());

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
	 * Is used to Choose the folder or a file, differenced by given Parameter.
	 * returns the folder/file-path or null if nothing where chosen or the
	 * Dialog was aborted
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
			createNewSet = createDataset.isSelected();
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
	 * Show the user a given ErrorMessage
	 * 
	 * @param message
	 */
	private void errorMessage(final String message) {
		JOptionPane.showMessageDialog(this, message, "Error",
				JOptionPane.ERROR_MESSAGE);
	}

	/**
	 * Creates the Filefilter for filechooser, in this case it filters for .nt
	 * .ttl .rdf types
	 * 
	 * @author shk2
	 * 
	 */
	class DataFilterFile extends javax.swing.filechooser.FileFilter {
		@Override
		public String getDescription() {
			return ".nt, .ttl, .rdf";
		}

		@Override
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
		@Override
		public String getDescription() {
			return ".store";
		}

		@Override
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
