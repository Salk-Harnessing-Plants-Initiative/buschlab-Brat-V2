package at.ac.oeaw.gmi.brat.gui.swing;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class NameSet{
	//get correlated image names (our naming scheme)
	public static ArrayList<ArrayList<String>> getSetNames(String[] strList) throws Exception{
		ArrayList<String> uniqueIds=new ArrayList<String>();
//		ArrayList<String> plateIds=new ArrayList<String>();
		for(int i=0;i<strList.length;++i){
			String[] strParts=strList[i].split("_");
			if(strParts.length==5){
				String uniqueId=strParts[1]+"_"+strParts[4];
//				String plateId=strParts[3];
				if(!uniqueIds.contains(uniqueId)){
					uniqueIds.add(uniqueId);
				}
			}
			else{
				String uniqueId=strParts[0]+"_"+strParts[3];
				if(!uniqueIds.contains(uniqueId)){
					uniqueIds.add(uniqueId);
				}
				
			}
		}
		ArrayList<ArrayList<String>> sets=new ArrayList<ArrayList<String>>();
		SortedMap<String,String> sortedFileNames=new TreeMap<String,String>();
		for(String uniqueString:uniqueIds){
			String[] strParts=uniqueString.split("_");
			for(int i=0;i<strList.length;++i){
				if(strList[i].contains(strParts[0]) && strList[i].contains(strParts[1])){
					String[] fileStrParts=strList[i].split("_");
					sortedFileNames.put(fileStrParts[2],strList[i]);
				}
			}
			ArrayList<String> setFileNames=new ArrayList<String>();
			for(Map.Entry<String,String> entry:sortedFileNames.entrySet()){
				setFileNames.add(entry.getValue());
			}
			sets.add(setFileNames);
		}
		return sets;
	}
	
	public static ArrayList<ArrayList<String>> getSetNames(String[] strList,String perSetConstRegEx){
		Pattern setPattern=Pattern.compile(perSetConstRegEx);
		ArrayList<String> perSetConstString=new ArrayList<String>();
		for(int i=0;i<strList.length;++i){
			Matcher setMatcher=setPattern.matcher(strList[i]);
			while(setMatcher.find()){
				if(!perSetConstString.contains(setMatcher.group())){
					perSetConstString.add(setMatcher.group());
				}
			}
		}
		ArrayList<ArrayList<String>> sets=new ArrayList<ArrayList<String>>();
		for(String constString:perSetConstString){
			ArrayList<String> setFileNames=new ArrayList<String>();
			for(int i=0;i<strList.length;++i){
				if(strList[i].contains(constString)){
					setFileNames.add(strList[i]);
				}
			}
			sets.add(setFileNames);
		}
		return sets;
	}

}
