package at.ac.oeaw.gmi.brat.gui.swing;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class SegmentationGUI extends JFrame{
	/**
	 * 
	 */
	private static final long serialVersionUID = -71299195471396493L;
	private FilePanel pnlFile;
	private LogPanel pnlLog;
	
	public SegmentationGUI(){
		initialize();
	}
	
	public LogPanel getLogPanel(){
		return pnlLog;
	}
	
	private void initialize(){
		setTitle("Brat Segmentation");
		pnlFile=new FilePanel();
		pnlLog=new LogPanel();
		
		Container cont=getContentPane();
		cont.setLayout(new BorderLayout());
		cont.add(pnlFile,BorderLayout.WEST);
		cont.add(pnlLog,BorderLayout.CENTER);
	}
	
	public void addActionListener(ActionListener l){
		pnlFile.addActionListener(l);
	}
	
	public void removeActionListener(ActionListener l){
		pnlFile.removeActionListener(l);
	}
	
	//testing
	public static void main(String[] args){ 
		SegmentationGUI gui=new SegmentationGUI();
		gui.setSize(1000,600);
		gui.setVisible(true);
	}

}

