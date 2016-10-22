package at.ac.oeaw.gmi.brat.utility;

import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ImageProcessor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

public class FileUtils {
	private final static Logger log=Logger.getLogger(FileUtils.class.getName());
	public static void assertFolder(final String folder) throws IOException {
		File f=new File(folder);
		if(!f.mkdirs()){
				throw new IOException("Could not create directory '"+folder+"'.");
		}
	}
	
	public static void createDir(final String dirName) throws IOException{
		File dir=new File(dirName);
		if(!dir.exists()){
			if(!dir.mkdir()){
				throw new IOException("Could not create directory '"+dirName+"'.");
			}
		}
	}

	public static void moveFile(final String src,final String dest){
		File srcFile=new File(src);
		File destFile=new File(dest);
		File destParentFile=destFile.getParentFile();
		try {
			assertFolder(destFile.getParent());
			if(!srcFile.renameTo(destFile)){
				throw new IOException(String.format("Could not rename file %s",src));
			}
		} catch (IOException e) {
			log.warning(String.format("could not move file %s\n%s",src,e.getMessage()));
//			e.printStackTrace();
		}
	}
	
//	public static void moveFile(final String sourceName,final String destName)throws IOException,SecurityException{
//		File sourceFile = new File(sourceName);
//		File destFile = new File(destName);
//
//		FileChannel source=null;
//		FileChannel destination=null;
//
//		try{
//			destFile.createNewFile();
//			source = new FileInputStream(sourceFile).getChannel();
//			destination = new FileOutputStream(destFile).getChannel();
//			destination.transferFrom(source,0,source.size());
//		}
//		finally{
//			if(source != null) source.close();
//			if(destination != null)	destination.close();
//		}
//		sourceFile.delete();
//	}
	
	public static String removeExtension(String s) {

	    String separator = System.getProperty("file.separator");
	    String filename;

	    // Remove the path upto the filename.
	    int lastSeparatorIndex = s.lastIndexOf(separator);
	    if (lastSeparatorIndex == -1) {
	        filename = s;
	    } else {
	        filename = s.substring(lastSeparatorIndex + 1);
	    }

	    // Remove the extension.
	    int extensionIndex = filename.lastIndexOf(".");
	    if (extensionIndex == -1)
	        return filename;

	    return filename.substring(0, extensionIndex);
	}

}
