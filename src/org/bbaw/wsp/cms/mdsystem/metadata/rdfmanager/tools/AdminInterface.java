package org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.tools;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.apache.log4j.Logger;
import org.bbaw.wsp.cms.collections.CollectionReader;
import org.bbaw.wsp.cms.mdsystem.metadata.rdfmanager.JenaMainForAI;

import com.hp.hpl.jena.reasoner.IllegalParameterException;

public class AdminInterface extends JFrame {

	/**
	 * Marco Seidler (shk2) tool for creation or appending a Dataset/Triplestore,
	 * with one or more rdf ttl nt file at the same time, also available is
	 * checking of already existing models and removing of models
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
	private JButton save_as_xml_btn;
	private JButton btn_larq;
	private Label des;
	private Label larq;
	private String srcD;
	private String desF;
	private String larqD;
	private JCheckBox allXML;
	private JCheckBox folderScr;
	private JCheckBox createDataset;
	private TextArea textArea;
	@SuppressWarnings("unused")
	private boolean createNewSet;
	@SuppressWarnings("rawtypes")
	private final JComboBox combobox = new JComboBox();
	private String namedGraph = "";
	private final JenaMainForAI jenaMain = new JenaMainForAI();
	private static Logger LOGGER = Logger.getLogger(CollectionReader.class);

	// ButtonsNames for distinction
	private static final String SOURCE_BUTTON = "Choose Metadata";
	private static final String DESTINATION_BUTTON = "Choose Destination";
	private static final String START_BUTTON = "Create new /add to Triplestore";
	private static final String LOAD_NAMEDMODELLS_BUTTON = "Load Models";
	private static final String REMOVE_BUTTON = "Remove";
	private static final String SAVE_AS_XML = "Save as XML";
	private static final String SET_NEW_LARQ = "Set new LarqIndex";

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
		LOGGER.info("AdminInterface started.");
	}

	/**
	 * Creates the Elements of GUI, adds Buttons with Buttonlistener and Labels,
	 * also adds value change listener on slider
	 */
	private void initalisation() {
		setLayout(new BorderLayout(5, 5));

		Panel panel = new Panel();
		panel.setLayout(new GridLayout(9, 1));
		btn_src = new JButton(SOURCE_BUTTON);
		src = new Label("no Source Data selected");
		btn_des = new JButton(DESTINATION_BUTTON);
		des = new Label("no Destination selected");
		btn_larq = new JButton(SET_NEW_LARQ);
		larq = new Label(jenaMain.readLarq());

		btn_go = new JButton(START_BUTTON);
		load_set_btn = new JButton(LOAD_NAMEDMODELLS_BUTTON);
		remove_btn = new JButton(REMOVE_BUTTON);
		save_as_xml_btn = new JButton(SAVE_AS_XML);
		combobox.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				namedGraph = (String) combobox.getSelectedItem();

			}
		});

		addButtonListener(btn_src);
		addButtonListener(btn_go);
		addButtonListener(btn_des);
		addButtonListener(remove_btn);
		addButtonListener(load_set_btn);
		addButtonListener(save_as_xml_btn);
		addButtonListener(btn_larq);

		panel.add(folderScr = new JCheckBox("Choose a source Folder"));
		panel
				.add(createDataset = new JCheckBox("Create a Dataset at new Location"));
		panel.add(btn_src);
		panel.add(src);
		panel.add(btn_des);
		panel.add(des);
		panel.add(btn_larq);
		panel.add(larq);

		panel.add(btn_go);

		textArea = new TextArea("Welcome,\n");

		Panel removePanel = new Panel();
		removePanel.setLayout(new GridLayout(7, 1));
		removePanel.add(new Label("Save/Remove Area"));
		removePanel.add(new Label("Load all Named Models from destination"));
		removePanel.add(load_set_btn);

		removePanel
				.add(allXML = new JCheckBox("Save all Models as XML to location"));
		removePanel.add(save_as_xml_btn);
		removePanel.add(remove_btn);
		removePanel.add(combobox);

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
				+ "The two Checkboxes:\n" + "1. is used to choose a whole folder\n "
				+ "where all possible data will be selected.\n"
				+ "This means also the Data in subfolders\nwill be selected.\n"
				+ "2. is used to create a new dataset,\n"
				+ "to do so there will open a save \n"
				+ "dialog in \"Choose Destination\" option\n\n"
				+ "If none of the Checkboxes is selected \n" + "the default is:\n"
				+ "choose 1 file to add to an existing dataset." + "\n\n"
				+ "Load Models lists all NamedModel which are\n"
				+ "currently in the choosen Dataset.\n\n"
				+ "The Larq Index must be set for the\n"
				+ "actually Dataset, if it is not the\n"
				+ " matching index an error will occur.\n"
				+ "To create a new LarqIndex just select\n"
				+ "an empty folder,as destination. \n\n"
				+ "The Export function gives the possible\n"
				+ "options to export a singel Model,\n"
				+ "or to export all to a given location.\n\n"
				+ "Remove deletes the choosen NamedModel\nfrom Dataset."
				+ "\n\nIf there already is a NamedModel or XML\n"
				+ "with equal name, it will be replaced\n"
				+ "by the newer version.\n\n"
				+ "To import an as XML extracted Model,\n"
				+ "just change the fileending \n"
				+ "to \".rdf\" instead of \".xml\"\n\n" + "Have a nice day.");

	}

	/**
	 * buttonlistener executes all the main Events differenced by which Button was
	 * pressed. Also exceptionhandling if wrong data were given
	 * 
	 * @param b
	 */
	private void addButtonListener(JButton b) {
		b.addActionListener(new ActionListener() {

			@Override
			@SuppressWarnings("unchecked")
			public void actionPerformed(ActionEvent ev) {
				if (ev.getActionCommand().equals(SET_NEW_LARQ)) {

					larqD = fileChooser(false, false);
					if (larqD != null) {
						larq.setText(larqD);
						try {
							jenaMain.setLarq(larqD);
						} catch (FileNotFoundException e) {
							// TODO Auto-generated catch block
							errorMessage(e.getMessage());
						} catch (IOException e) {
							// TODO Auto-generated catch block
							errorMessage(e.getMessage());
						}

					}

				} else if (ev.getActionCommand().equals(SOURCE_BUTTON)) {

					if (folderScr.isSelected()) {
						srcD = fileChooser(false, false);
						scanforRDF(srcD);
					} else
						srcD = fileChooser(true, false);
					if (srcD == null) {
						src.setText("no Source Folder selected");
					} else
						src.setText(srcD);
					LOGGER.info(srcD + " as source selected.");
				} else if (ev.getActionCommand().equals(DESTINATION_BUTTON)) {

					desF = fileChooser(true, true);
					if (desF == null) {
						des.setText("no Destination selected");
					} else
						des.setText(desF);

					LOGGER.info(des + " as source selected.");
				} else if (ev.getActionCommand().equals(LOAD_NAMEDMODELLS_BUTTON)) {
					getModels();

				} else if (ev.getActionCommand().equals(REMOVE_BUTTON)) {
					if (desF == null) {
						println("no source choosed!");
						return;
					} else if (combobox.getItemCount() == 0) {
						println("No Models available");
						return;
					}

					jenaMain.setDestination(desF);

					jenaMain.removeModel(namedGraph);
					println(namedGraph + " successfully removed.");
					println("Index will refresh now.");
					combobox.removeAllItems();
					for (String s : jenaMain.getModels()) {
						combobox.addItem(s);
					}

				} else if (ev.getActionCommand().equals(SAVE_AS_XML)) {
					if (combobox.getItemCount() == 0) {
						println("No Models available");
						return;
					}
					jenaMain.setDestination(desF);
					String file = fileChooserXML();
					if (file != null) {
						if (file.endsWith(".xml")) {
							jenaMain.saveModel(namedGraph, file);
							println(file + " \nsaved successfully.");
						} else {
							jenaMain.saveAllModel(file);
							println("All Model were saved successfully in\n" + file);
						}
					}
					return;
				}

				else if (ev.getActionCommand().equals(START_BUTTON)) {

					if (desF == null) {
						println("no destination choosed!");
						return;
					}
					if (srcD != null) {
						println("Choosen Data: " + srcD);
					}
					println("Choosen Destination" + desF);

					try {
						execution();
						getModels();
						println("finished!");

					} catch (Exception e) {
						println(e.getMessage());
						errorMessage(e.getMessage());
						LOGGER.error(e.getMessage());
					}

				}

			}
		});
	}

	/**
	 * Methode asks Triplestore for containing Models
	 */
	@SuppressWarnings("unchecked")
	private void getModels() {
		if (desF == null) {
			println("no Destination selected");
			return;
		}
		jenaMain.setDestination(desF);
		try {
			ArrayList<String> models = jenaMain.getModels();

			if (models.isEmpty()) {
				println("There are no models in the choosen Dataset\n" + desF);
				return;
			}
			for (String s : models) {
				if (checkAlreadyinList(s))
					combobox.addItem(s);

			}
			combobox.showPopup();

			println("Loaded all named Graphes from " + desF);
			namedGraph = (String) combobox.getItemAt(0);
		} catch (IllegalParameterException e) {
			errorMessage(e.getMessage());
		}
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

		if (srcD != null)
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

		if (str == null)
			return null;
		File dir = new File(str);
		ArrayList<File> liste = new ArrayList<File>();

		ArrayList<String> folderList = new ArrayList<String>();
		folderList.add(str);
		folderList = scanFolder(folderList, dir);
		println("\n");
		for (String path : folderList) {
			println("Current directory is " + path);

			println("Valid Files in the Folder:\n");
			for (File f : new File(path).listFiles()) {
				if (f.getName().toLowerCase().endsWith(".rdf")
						|| f.getName().toLowerCase().endsWith(".ttl")
						|| f.getName().toLowerCase().endsWith(".nt")) {
					liste.add(f);
					println(" - " + f.getName());
				}
			}
			println("\n");
		}

		return liste;
	}

	/**
	 * Methoded lists all folders in a given dir and returns them, needs a list to
	 * write in & a root folder
	 * 
	 * @param folderlist
	 * @param root
	 * @return
	 */
	private ArrayList<String> scanFolder(ArrayList<String> folderlist, File root) {

		ArrayList<File> tempFolderList = new ArrayList<File>();
		for (File f : root.listFiles()) {
			if (f.isDirectory()) {
				tempFolderList.add(f);
			}
		}

		for (File f : tempFolderList) {
			scanFolder(folderlist, f);
			folderlist.add(f.getAbsolutePath());

		}

		return folderlist;
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
	 * returns the folder/file-path or null if nothing where chosen or the Dialog
	 * was aborted
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
			try {
				String file = chooser.getSelectedFile().getAbsolutePath();

				if (save) {

					if (!file.toLowerCase().endsWith(".store")) {
						file += ".store";

					}
				}

				return file;
			} catch (Exception e) {

			}

		}
		return null;
	}

	/**
	 * Methode whitch opens a dialog to choose a folder or a file for saving a
	 * given model(s)
	 * 
	 * @return
	 */
	private String fileChooserXML() {
		JFileChooser chooser = new JFileChooser();
		int returnVal = 0;
		Boolean directorys = allXML.isSelected();
		if (directorys) {
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		} else
			chooser.setFileFilter(new XMLFilterFile());

		chooser.showSaveDialog(chooser);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			try {
				String file = chooser.getSelectedFile().getAbsolutePath();

				if (!directorys) {
					if (!file.toLowerCase().endsWith(".xml")) {
						file += ".xml";

					}

				}
				LOGGER.info("Choosed " + file + " for export as XML");
				return file;
			} catch (Exception e) {

			}
		}
		return null;

	}

	/**
	 * Show the user a given ErrorMessage
	 * 
	 * @param message
	 */
	private void errorMessage(final String message) {
		LOGGER.error(message);
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
	 * New Filter which is used to choose xml Files
	 * 
	 * @author shk2
	 * 
	 */
	class XMLFilterFile extends javax.swing.filechooser.FileFilter {

		@Override
		public boolean accept(File file) {
			if (file.isDirectory())
				return true;
			else if (file.getName().toLowerCase().endsWith(".xml"))
				return true;
			else
				return false;
		}

		@Override
		public String getDescription() {

			return ".xml";
		}

	}

	/**
	 * Creates the Filefilter for filechooser, in this case it filters for .store
	 * types
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
}
