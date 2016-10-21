package at.ac.oeaw.gmi.brat.gui.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class LogPanel extends JPanel implements ActionListener,AdjustmentListener{
	/**
	 * 
	 */
	private static final long serialVersionUID = 6377116904349585952L;
	private JTextPane pneLog;
	private JScrollPane pneScroll;
	private JButton btnWriteLog;
	
	public LogPanel(){
		initialize();
	}
	
	public JTextPane getLogPane(){
		return pneLog;
	}
	
	public void appendText(String text,SimpleAttributeSet attributes){
		try{
			Document doc=pneLog.getDocument();
			doc.insertString(doc.getLength(),text,attributes);
		} catch (BadLocationException e){
			// should not happen
			e.printStackTrace();
		}
		pneLog.repaint();
		
		scrollToBottom();
	}
	

	private void scrollToBottom()
	{
	    SwingUtilities.invokeLater(
	        new Runnable()
	        {
	            public void run()
	            {
	                pneScroll.getVerticalScrollBar().setValue(pneScroll.getVerticalScrollBar().getMaximum());
	            }
	        });
	}
	
	private void initialize(){
		setLayout(new BorderLayout());
		pneLog=new JTextPane();
		pneLog.setEditable(false);
		TitledBorder brdScrllPne=new TitledBorder("Log"){
			/**
			 * 
			 */
			private static final long serialVersionUID = 4974761451672539769L;
			private Insets customInsets=new Insets(20,5,5,5);
			public Insets getBorderInsets(Component c){
				return customInsets;
			}
		};
		setBorder(brdScrllPne);
		pneScroll=new JScrollPane(pneLog);
	    pneScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pneScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
//		pneScroll.setBorder(brdScrllPne);
		pneScroll.setPreferredSize(new Dimension(400,400)); //Short.MAX_VALUE));
//		pneScroll.getVerticalScrollBar().addAdjustmentListener(this);
		add(pneScroll,BorderLayout.CENTER);

		
		JPanel pnlLogAction = new JPanel();
		pnlLogAction.setBorder(new EmptyBorder(2,10,4,10));
		add(pnlLogAction, BorderLayout.SOUTH);
		
		btnWriteLog = new JButton("Write Log");
		btnWriteLog.addActionListener(this);
		pnlLogAction.add(btnWriteLog);
	}
	
	//testing
	public static void main(String[] args){ 
		JFrame frame = new JFrame();
		frame.setBounds(100, 100, 1000, 618);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());
		
		JPanel pnlMain = new JPanel();
		frame.getContentPane().add(pnlMain,BorderLayout.CENTER);
		pnlMain.setLayout(new BorderLayout());
//		pnlMain.setMaximumSize(new Dimension(400,Short.MAX_VALUE));
		pnlMain.add(new LogPanel(),BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if(e.getSource()==btnWriteLog){
			JFileChooser chooser = new JFileChooser();
//			chooser.setCurrentDirectory(new File(txtBaseDir.getText()));
			chooser.setDialogTitle("write log to file");
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setAcceptAllFileFilterUsed(false);

			if(chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION){
				File selectedFile=chooser.getSelectedFile();
				BufferedWriter ofile=null;
				try{
					ofile=new BufferedWriter(new FileWriter(selectedFile));
					ofile.write(pneLog.getText());
				} catch (IOException ioe) {
					ioe.printStackTrace();
				}
				finally{
					if(ofile!=null){
						try {
							ofile.close();
						} catch (IOException e1) {
							e1.printStackTrace();
						}
					}
				}
			}
		}
	}

	@Override
	public void adjustmentValueChanged(AdjustmentEvent e) {
		scrollToBottom();
		
	}
}
