package at.ac.oeaw.gmi.brat.gui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

public class FilePanel extends JPanel implements ActionListener,ComponentListener,ItemListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = -5661688025441757843L;
//	private final String[] defaultStrings={"(^.*?_)","set\\d+","_\\d{3}$","day\\d+",".tif"};
	private final String[] stPtOptStrings={"none","first avail","last avail","average"};
	
	private JTextField[] txtIdentifiers;
	
	private JTextField txtBaseDir;
	private JButton btnBaseDirSelect;
	private JButton btnBaseDirStart;
	private JButton btnBaseDirReset;
	
	private JCheckBox[] chkOptions;
	private JComboBox<String> cbxStptOption;
	private int stPtPrevSelection;
	private JProgressBar progressBar;
	
	final private Preferences prefs = Preferences.userRoot().node("at/ac/oeaw/gmi/bratv2");
	
	final private List<ActionListener> externalListeners;
	
	public FilePanel(){
		this.externalListeners=new ArrayList<ActionListener>();
		initialize();
	}
	
	public void addActionListener(ActionListener l){
		externalListeners.add(l);
	}
	
	public void removeActionListener(ActionListener l){
		externalListeners.remove(l);
	}

	private void initialize(){
		TitledBorder brdIdentifiers=new TitledBorder("Identifiers"){
			/**
			 * 
			 */
			private static final long serialVersionUID = 4974761451672539769L;
			private Insets customInsets=new Insets(25,15,15,15);
			public Insets getBorderInsets(Component c){
				return customInsets;
			}
		};
		
		JPanel pnlIdentifiers=new JPanel(new GridBagLayout());
		pnlIdentifiers.setBorder(brdIdentifiers);
		
		GridBagConstraints gbc=new GridBagConstraints();
		gbc.weighty=1.0;
		gbc.fill=GridBagConstraints.BOTH;
		gbc.anchor=GridBagConstraints.WEST;
		gbc.ipadx=5;
		gbc.ipady=5;
		
		JLabel[] lblId=new JLabel[5];
		lblId[0]=new JLabel("File");
		lblId[1]=new JLabel("Set");
		lblId[2]=new JLabel("Plate");
		lblId[3]=new JLabel("Day");
		lblId[4]=new JLabel("File");
		
		JLabel[] lblId2=new JLabel[5];
		lblId2[0]=new JLabel("Identifier:");
		lblId2[1]=new JLabel("Identifier:");
		lblId2[2]=new JLabel("Identifier:");
		lblId2[3]=new JLabel("Identifier:");
		lblId2[4]=new JLabel("Extension:");
		
		txtIdentifiers=new JTextField[5];
		gbc.weighty=1.0;
		gbc.anchor=GridBagConstraints.EAST;
		for(int i=4;i<5;++i){
			gbc.gridy=i;
			gbc.gridx=0;
			gbc.weightx=0.2;
			pnlIdentifiers.add(lblId[i],gbc);
			gbc.gridx=1;
			gbc.weightx=0.2;
			pnlIdentifiers.add(lblId2[i],gbc);
			txtIdentifiers[i]=new JTextField(20);
			txtIdentifiers[i].setMargin(new Insets(2,4,2,4));
			gbc.gridx=2;
			gbc.weightx=1.0;
			txtIdentifiers[i].setText(prefs.get("fileExtension","tif"));
			pnlIdentifiers.add(txtIdentifiers[i],gbc);
		}
		
		
		// panel base dir
		TitledBorder brdBaseDir=new TitledBorder("Base Directory"){
			/**
			 * 
			 */
			private static final long serialVersionUID = 4974761451672539769L;
			private Insets customInsets=new Insets(25,15,15,15);
			public Insets getBorderInsets(Component c){
				return customInsets;
			}
		};
		JPanel pnlBaseDir=new JPanel(new GridBagLayout());
		pnlBaseDir.setBorder(brdBaseDir);
		gbc=new GridBagConstraints();
		
		gbc.gridx=0;
		gbc.gridy=0;
		gbc.gridwidth=3;
		gbc.weightx=1.0;
		gbc.weighty=1.0;
		gbc.fill=GridBagConstraints.BOTH;
		txtBaseDir=new JTextField(30);
		txtBaseDir.setMargin(new Insets(3,4,2,4));
		txtBaseDir.setText(prefs.get("baseDirectory",System.getProperty("user.home")));
//		txtBaseDir.setCaretPosition(txtBaseDir.getText().trim().length());
		pnlBaseDir.add(txtBaseDir,gbc);
		
		gbc.gridx=3;
		gbc.weightx=0.2;
		gbc.gridwidth=1;
		btnBaseDirSelect=new JButton("...");
		btnBaseDirSelect.setPreferredSize(new Dimension(30,25));
		btnBaseDirSelect.setMaximumSize(new Dimension(30,25));
		btnBaseDirSelect.addActionListener(this);
		pnlBaseDir.add(btnBaseDirSelect,gbc);
		pnlBaseDir.addComponentListener(this);
		// panel progress
		JPanel pnlProgress=new JPanel(new GridBagLayout());
		pnlProgress.setBorder(new EmptyBorder(10,14,10,14));
//		TitledBorder brdProgress=new TitledBorder("Progress"){
//			/**
//			 * 
//			 */
//			private static final long serialVersionUID = 4974761451672539769L;
//			private Insets customInsets=new Insets(25,15,15,15);
//			public Insets getBorderInsets(Component c){
//				return customInsets;
//			}
//		};
//		pnlProgress.setBorder(brdProgress);

//		gbc.gridx=1;
//		gbc.gridy=1;
//		gbc.weightx=0.2;
//		gbc.fill=GridBagConstraints.HORIZONTAL;
//		btnBaseDirReset=new JButton("Reset");
//		btnBaseDirReset.setPreferredSize(new Dimension(60,25));
//		btnBaseDirReset.setMaximumSize(new Dimension(60,25));
//		btnBaseDirReset.addActionListener(this);
//		pnlProgress.add(btnBaseDirReset,gbc);
		
		gbc.gridx=2;
		gbc.gridy=1;
		gbc.weightx=0.0;
		gbc.fill=GridBagConstraints.HORIZONTAL;
		btnBaseDirStart=new JButton("Start");
		btnBaseDirStart.setPreferredSize(new Dimension(90,25));
		btnBaseDirStart.setMaximumSize(new Dimension(90,25));
		btnBaseDirStart.addActionListener(this);
		pnlProgress.add(btnBaseDirStart,gbc);
		
		
//		gbc.gridx=0;
//		gbc.weightx=1.0;
//		pnlProgress.add(new JPanel(),gbc); //empty Panel
//		
//		gbc.gridx=0;
//		gbc.gridy=2;
//		gbc.gridwidth=3;
//		gbc.weightx=1.0;
//		gbc.fill=GridBagConstraints.HORIZONTAL;
//		gbc.gridwidth=3;
//		gbc.ipady=7;
//		gbc.insets=new Insets(10,0,0,0);
//		progressBar=new JProgressBar(0,100);
//		progressBar.setValue(0);
//		progressBar.setStringPainted(true);
//		progressBar.setString("idle");
//		pnlProgress.add(progressBar,gbc);

		TitledBorder brdOptions=new TitledBorder("Options"){
			/**
			 * 
			 */
			private static final long serialVersionUID = 4974761451672539769L;
			private Insets customInsets=new Insets(25,15,15,15);
			public Insets getBorderInsets(Component c){
				return customInsets;
			}
		};
		chkOptions=new JCheckBox[3];
		chkOptions[0]=new JCheckBox("flip horizontal",true);
		chkOptions[1]=new JCheckBox("equalize histogram",false);
		chkOptions[2]=new JCheckBox("process time series",true);
		chkOptions[2].addItemListener(this);
		
		gbc=new GridBagConstraints();
		JPanel pnlOptions=new JPanel(new GridBagLayout());
		pnlOptions.setBorder(brdOptions);
		for(int i=0;i<chkOptions.length-1;++i){
			gbc.gridx=0;
			gbc.gridy=i;
			gbc.anchor=GridBagConstraints.WEST;
			pnlOptions.add(chkOptions[i],gbc);
		}
		gbc.gridx=2;
		gbc.gridy=0;
		pnlOptions.add(chkOptions[chkOptions.length-1],gbc);
		
		cbxStptOption=new JComboBox<>(stPtOptStrings);
		cbxStptOption.setSelectedIndex(3);
		JPanel cbxPanel=new JPanel(new BorderLayout());
		cbxPanel.setBorder(new EmptyBorder(0,20,0,0));
		cbxPanel.add(new JLabel("Start pt:"),BorderLayout.WEST);
		cbxPanel.add(cbxStptOption,BorderLayout.CENTER);
		
		gbc.gridx=2;
		gbc.gridy=1;
		gbc.gridheight=2;
		gbc.fill=GridBagConstraints.HORIZONTAL;
		pnlOptions.add(cbxPanel,gbc);
//		gbc.gridx=1;
//		gbc.gridy=0;
//		gbc.weighty=0.9;
//		gbc.fill=GridBagConstraints.VERTICAL;
//		gbc.gridheight=chkStPt.length;
//		JSeparator sep=new JSeparator(JSeparator.VERTICAL);
//		sep.setPreferredSize(new Dimension(5,1));
//		pnlOptions.add(sep,gbc);
		
		
		JPanel pnlCenter=new JPanel();
		pnlCenter.setLayout(new BoxLayout(pnlCenter,BoxLayout.Y_AXIS));
		pnlCenter.add(pnlIdentifiers);
		pnlCenter.add(pnlBaseDir);
		pnlCenter.add(pnlOptions);
		
		setLayout(new BorderLayout());
		add(pnlCenter,BorderLayout.NORTH);
		add(pnlProgress,BorderLayout.SOUTH);
	}
	
	private void updateConfiguration(){
		String tmpBase=txtBaseDir.getText();
		if(!tmpBase.endsWith(System.getProperty("file.separator"))){
			tmpBase+=System.getProperty("file.separator");
		}
		prefs.put("baseDirectory",tmpBase);
		prefs.put("outputDirectory",new File(tmpBase,"processed").getAbsolutePath());
		prefs.put("moveDirectory",new File(tmpBase,"processed-tif").getAbsolutePath());

		prefs.put("fileExtension",txtIdentifiers[4].getText());
		
		prefs.putBoolean("useSets",chkOptions[2].isSelected());
		prefs.putInt("commonStartPtMode",cbxStptOption.getSelectedIndex());
		
		prefs.putBoolean("flipHorizontal",chkOptions[0].isSelected());
		prefs.putBoolean("enhanceImage",chkOptions[1].isSelected());
		
		prefs.put("nThreads","1"); //TODO nThreads is fixed to 1 atm
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==btnBaseDirSelect){
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File(txtBaseDir.getText()));
			chooser.setDialogTitle("Choose the base directory");
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setAcceptAllFileFilterUsed(false);

			if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
				File selectedFile=chooser.getSelectedFile();
				if(selectedFile.isDirectory())
					txtBaseDir.setText(selectedFile.getAbsolutePath());
				else
					txtBaseDir.setText(selectedFile.getParent());
			}
		}
		else if(e.getSource()==btnBaseDirReset){
			ActionEvent ae=new ActionEvent(getClass(),ActionEvent.ACTION_PERFORMED,"reset");
			for(ActionListener l:externalListeners){
				l.actionPerformed(ae);
			}
		}
		else if(e.getSource()==btnBaseDirStart){
			updateConfiguration();
			ActionEvent ae=new ActionEvent(getClass(),ActionEvent.ACTION_PERFORMED,"start");
			for(ActionListener l:externalListeners){
				l.actionPerformed(ae);
			}
		}
		
	}

	@Override
	public void componentResized(ComponentEvent e) {
		txtBaseDir.setCaretPosition(txtBaseDir.getText().trim().length());
		btnBaseDirStart.requestFocusInWindow();
	}

	@Override
	public void componentMoved(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void componentShown(ComponentEvent e) {
//		txtBaseDir.setCaretPosition(txtBaseDir.getText().trim().length());
//		btnBaseDirStart.requestFocusInWindow();
	}

	@Override
	public void componentHidden(ComponentEvent e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if(e.getSource()==chkOptions[2]){
			if(e.getStateChange()==ItemEvent.DESELECTED){
				stPtPrevSelection=cbxStptOption.getSelectedIndex();
				cbxStptOption.setSelectedIndex(0);
				cbxStptOption.setEnabled(false);
			}
			else{
				cbxStptOption.setSelectedIndex(stPtPrevSelection);
				cbxStptOption.setEnabled(true);
			}
		}
	}

}
