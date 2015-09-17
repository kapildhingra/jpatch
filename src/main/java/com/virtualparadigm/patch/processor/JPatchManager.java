package com.virtualparadigm.patch.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.virtualparadigm.patch.util.ZipUtils;

public class JPatchManager
{
	private static Logger logger = LoggerFactory.getLogger(JPatchManager.class);
	
	private static final String ELEMENT_PATCH = "patch";
	private static final String ELEMENT_ADDED_FILES = "addedFiles";
	private static final String ELEMENT_UPDATED_FILES = "udpatedFiles";
	private static final String ELEMENT_REMOVED_FILES = "removedFiles";
	private static final String ELEMENT_ADDED_DIRECTORIES = "addedDirectories";
	private static final String ELEMENT_REMOVED_DIRECTORIES = "removedDirectories";
	private static final String ELEMENT_DIRECTORY = "directory";
	private static final String ELEMENT_FILE = "file";
	private static final String ATTRIBUTE_PATH = "path";
	private static final String ATTRIBUTE_EXPECTED_LAST_MODIFIED = "expectedLastModified";
	
    private static final String PATCH_FILE_NAME = "patch.xml";
    private static final String PATCH_FILES_DIR_NAME = "patch-files";
    
    private static final String FILE_LIST_KEY = "fileList";
    private static final String DIR_LIST_KEY = "directoryList";
	
	
	public JPatchManager()
	{
		super(); 
	}
	
	public static void main(String[] args) throws IOException
	{
		
		
	}

	public Document makePatch(File oldStateRootDir, File newStateRootDir, File outputDir, String archiveFileName)// throws IOException
	{
		Document patchDocument = DocumentHelper.createDocument();
		Element patchElement = patchDocument.addElement(ELEMENT_PATCH);
		patchElement.addElement(ELEMENT_ADDED_FILES);
		patchElement.addElement(ELEMENT_UPDATED_FILES);
		patchElement.addElement(ELEMENT_REMOVED_FILES);
		patchElement.addElement(ELEMENT_ADDED_DIRECTORIES);
		patchElement.addElement(ELEMENT_REMOVED_DIRECTORIES);
		
		try
		{
			//if prepare fails (IOException), doesn't make sense to go further
	        JPatchManager.prepareDir(outputDir, false);
			File patchFile = new File(outputDir.getAbsolutePath() + "/" + JPatchManager.PATCH_FILE_NAME);
			File patchFilesDir = new File(outputDir.getAbsolutePath() + "/" + JPatchManager.PATCH_FILES_DIR_NAME);
			
			patchDocument = this.createDirectoryPatch(oldStateRootDir, "/", newStateRootDir, "/", patchFilesDir, patchDocument);
			JPatchManager.writeDocumentToFile(patchDocument, patchFile);
			
			if(archiveFileName != null && archiveFileName.length() > 0)
			{
				ZipUtils.createZipFile(outputDir.getAbsolutePath() + "/" + archiveFileName, new File[]{patchFile, patchFilesDir}, 1024);
				FileUtils.deleteQuietly(patchFile);
				FileUtils.deleteQuietly(patchFilesDir);
			}
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		return patchDocument;
	}
	
	
	private Document createDirectoryPatch(File oldStateRootDir, String oldStateRelDirPath, File newStateRootDir, String newStateRelDirPath, File patchFilesRootDir, Document patchDocument)
	{
		if(newStateRelDirPath != null && patchDocument != null)
		{
            List<File> addedFileList = new ArrayList<File>();
            List<FileTuple> updatedFileTupleList = new ArrayList<FileTuple>();
            List<File> removedFileList = new ArrayList<File>();
            List<File> addedDirList = new ArrayList<File>();
            List<File> removedDirList = new ArrayList<File>();
            
			File newStateDir = new File(newStateRootDir.getAbsolutePath() + File.separator + newStateRelDirPath);

			if(oldStateRelDirPath == null)
			{
				//add all of the files from new state relative directory
				File outputDir = new File(patchFilesRootDir.getAbsolutePath() + File.separator + newStateRelDirPath);
				if(JPatchManager.forceMakeDirectory(outputDir))
				{
				    addedDirList.add(outputDir);

                    List<File> newStateFileList = null;
                    List<File> newStateSubDirList = null;
                    
					Map<String, List<File>> categorizedFileMap = JPatchManager.categorizeFiles(newStateDir.listFiles());
					if(categorizedFileMap != null)
					{
                        newStateFileList = categorizedFileMap.get(JPatchManager.FILE_LIST_KEY);
                        newStateSubDirList = categorizedFileMap.get(JPatchManager.DIR_LIST_KEY);
					}
					
					if(newStateFileList != null && newStateFileList.size() > 0)
					{
                        for(File newStateFile : newStateFileList)
                        {
                            if(JPatchManager.forceCopyFileToDirectory(newStateFile, outputDir))
                            {
                                addedFileList.add(new File(outputDir + File.separator + newStateFile.getName()));
                            }
                        }
					}
					
                    if(newStateSubDirList != null && newStateSubDirList.size() > 0)
                    {
                        for(File newStateSubDir : newStateSubDirList)
                        {
                            this.createDirectoryPatch(
                                    oldStateRootDir, 
                                    null, 
                                    newStateRootDir, 
                                    newStateRelDirPath + File.separator + newStateSubDir.getName(), 
                                    patchFilesRootDir, 
                                    patchDocument);
                        }
                    }
				}
			}
			else 
			{
				if(newStateRelDirPath.equals(oldStateRelDirPath))
				{
					File oldStateDir = new File(oldStateRootDir.getAbsolutePath() + File.separator + oldStateRelDirPath);
					
                    List<File> newStateFileList = null;
                    List<File> newStateSubDirList = null;
                    
                    Map<String, List<File>> categorizedFileMap = JPatchManager.categorizeFiles(newStateDir.listFiles());
                    if(categorizedFileMap != null)
                    {
                        newStateFileList = categorizedFileMap.get(JPatchManager.FILE_LIST_KEY);
                        newStateSubDirList = categorizedFileMap.get(JPatchManager.DIR_LIST_KEY);
                    }
					
                    Map<String, File> oldStateFileMap = new HashMap<String, File>();
                    Map<String, File> oldStateDirMap = new HashMap<String, File>();
                    
                    if(oldStateDir.exists())
                    {
    					for(File oldStateFile : oldStateDir.listFiles())
    					{
    					    if(oldStateFile.isDirectory())
    					    {
    	                        oldStateDirMap.put(oldStateFile.getName(), oldStateFile);
    					    }
    					    else
    					    {
                                oldStateFileMap.put(oldStateFile.getName(), oldStateFile);
    					    }
    					}
                    }
//                    try
//                    {
//                    }
//                    catch(Throwable t)
//                    {
//                    	//could not access oldStateDir (might not exist or throws security exception)
//                    }

                    File foundOldStateFile = null;
                    File foundOldStateDir = null;
                    
                    if(newStateFileList != null)
                    {
                        for(int newStateFileIndex=0; newStateFileIndex < newStateFileList.size(); newStateFileIndex++)
                        {
                            foundOldStateFile = oldStateFileMap.get(newStateFileList.get(newStateFileIndex).getName());
                            if(foundOldStateFile == null)
                            {
                                File outputDir = new File(patchFilesRootDir.getAbsolutePath() + File.separator + newStateRelDirPath);
                                if(JPatchManager.forceCopyFileToDirectory(newStateFileList.get(newStateFileIndex), outputDir))
                                {
                                    addedFileList.add(new File(outputDir + File.separator + newStateFileList.get(newStateFileIndex).getName()));
                                }
                            }
                            else
                            {
                            	//found oldstate file with same name
                                if(!checksum(foundOldStateFile).equals(checksum(newStateFileList.get(newStateFileIndex))))
                                {
                                    //IF CHECKSUM IS NOT THE SAME BUT LAST MODIFIED IS, KEEP THE OLD FILE
                                    if(foundOldStateFile.lastModified() >= newStateFileList.get(newStateFileIndex).lastModified())
                                    {
                                    	//if old state is newer, why even copy this file? just assume dest is >= to the old state version
                                        oldStateFileMap.remove(foundOldStateFile.getName());
                                        foundOldStateFile = null;
                                    }
                                    else if(newStateFileList.get(newStateFileIndex).lastModified() > foundOldStateFile.lastModified())
                                    {
                                        File outputDir = new File(patchFilesRootDir.getAbsolutePath() + File.separator + newStateRelDirPath);
                                        JPatchManager.forceCopyFileToDirectory(newStateFileList.get(newStateFileIndex), outputDir);

                                        updatedFileTupleList.add(
                                        		new FileTuple(
                                        				new File(outputDir + File.separator + newStateFileList.get(newStateFileIndex).getName()), 
                                        				foundOldStateFile.lastModified()));
                                        
                                        oldStateFileMap.remove(foundOldStateFile.getName());
                                        foundOldStateFile = null;
                                    }
                                }
                                else
                                {
                                    oldStateFileMap.remove(foundOldStateFile.getName());
                                    foundOldStateFile = null;
                                }
                            }
                        }
                    }
                    
                    if(newStateSubDirList != null)
                    {
                        for(int newStateSubDirIndex=0; newStateSubDirIndex < newStateSubDirList.size(); newStateSubDirIndex++)
                        {
                            foundOldStateDir = oldStateDirMap.get(newStateSubDirList.get(newStateSubDirIndex).getName());
                            if(foundOldStateDir == null)
                            {
                                this.createDirectoryPatch(
                                        oldStateRootDir, 
                                        null, 
                                        newStateRootDir, 
                                        newStateRelDirPath + File.separator + newStateSubDirList.get(newStateSubDirIndex).getName(), 
                                        patchFilesRootDir, 
                                        patchDocument);
                            }
                            else
                            {
                                this.createDirectoryPatch(
                                        oldStateRootDir, 
                                        oldStateRelDirPath + File.separator + foundOldStateDir.getName(), 
                                        newStateRootDir, 
                                        newStateRelDirPath + File.separator + newStateSubDirList.get(newStateSubDirIndex).getName(), 
                                        patchFilesRootDir, 
                                        patchDocument);
                            
                                //need this? YES: b/c every thing left will be in the removed!
                                oldStateDirMap.remove(newStateSubDirList.get(newStateSubDirIndex).getName());
                                newStateSubDirList.set(newStateSubDirIndex, null);
                            }
                        }
                    }
                    
                    removedFileList.addAll(oldStateFileMap.values());
                    removedDirList.addAll(oldStateDirMap.values());
				}
			}
			
            for(File addedDir : addedDirList)
            {
            	JPatchManager.addPathElement(
            			patchDocument, 
            			ELEMENT_PATCH + "/" + ELEMENT_ADDED_DIRECTORIES, 
            			ELEMENT_DIRECTORY, 
            			ATTRIBUTE_PATH, 
            			JPatchManager.getRelativePath(addedDir.getAbsolutePath(), patchFilesRootDir.getAbsolutePath()) + "/");
            }
			
            for(File removedDir : removedDirList)
            {
            	JPatchManager.addPathElement(
            			patchDocument, 
            			ELEMENT_PATCH + "/" + ELEMENT_REMOVED_DIRECTORIES, 
            			ELEMENT_DIRECTORY, 
            			ATTRIBUTE_PATH, 
            			JPatchManager.getRelativePath(removedDir.getAbsolutePath(), oldStateRootDir.getAbsolutePath()) + "/", 
            			removedDir.lastModified());
            }
			
            for(File addedFile : addedFileList)
            {
            	JPatchManager.addPathElement(
            			patchDocument, 
            			ELEMENT_PATCH + "/" + ELEMENT_ADDED_FILES, 
            			ELEMENT_FILE, 
            			ATTRIBUTE_PATH, 
            			JPatchManager.getRelativePath(addedFile.getAbsolutePath(), patchFilesRootDir.getAbsolutePath()));
            }
			
            for(FileTuple updatedFileTuple : updatedFileTupleList)
            {
            	JPatchManager.addPathElement(
            			patchDocument, 
            			ELEMENT_PATCH + "/" + ELEMENT_UPDATED_FILES, 
            			ELEMENT_FILE, 
            			ATTRIBUTE_PATH, 
            			JPatchManager.getRelativePath(updatedFileTuple.getFile().getAbsolutePath(), patchFilesRootDir.getAbsolutePath()), 
            			updatedFileTuple.getExpectedLastModified());
            }
            
            for(File removedFile : removedFileList)
            {
            	JPatchManager.addPathElement(
            			patchDocument, 
            			ELEMENT_PATCH + "/" + ELEMENT_REMOVED_FILES, 
            			ELEMENT_FILE, 
            			ATTRIBUTE_PATH, 
            			JPatchManager.getRelativePath(removedFile.getAbsolutePath(), oldStateRootDir.getAbsolutePath()), 
            			removedFile.lastModified());
            }
            
		}
		return patchDocument;
	}

	public void executePatch(File archiveFile, File tempDir, File targetRootDir, Matcher excludeMatcher, File rollbackRootDir, boolean insertsOnly, boolean forceUpdates)
	{
		try
		{
			JPatchManager.prepareDir(tempDir, true);
			ZipUtils.unzipArchive(archiveFile, tempDir);
			this.executePatch(tempDir, targetRootDir, excludeMatcher, rollbackRootDir, insertsOnly, forceUpdates);
			FileUtils.deleteQuietly(tempDir);
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
	}

//	public void executePatch(File patchRootDir, File targetRootDir, File rollbackRootDir, File rollbackArchiveFile, boolean insertsOnly, boolean forceUpdates)
//	{
//		try
//		{
//			JPatchManager.prepareDir(tempDir);
//			this.executePatch(tempDir, targetRootDir, rollbackRootDir, insertsOnly, forceUpdates);
//			FileUtils.deleteQuietly(tempDir);
//		}
//		catch(IOException ioe)
//		{
//			ioe.printStackTrace();
//		}
//	}
	
	public void executePatch(File patchRootDir, File targetRootDir, Matcher excludeMatcher, File rollbackPatchRootDir, boolean insertsOnly, boolean forceUpdates)
	{
		try
		{
			File patchFile = new File(patchRootDir.getAbsolutePath() + "/" + JPatchManager.PATCH_FILE_NAME);
			File patchFilesDir = new File(patchRootDir.getAbsolutePath() + "/" + JPatchManager.PATCH_FILES_DIR_NAME);
			File rollbackPatchFile = null;
			File rollbackPatchFilesDir = null;
			
			if(rollbackPatchRootDir != null)
			{
				rollbackPatchFile = new File(rollbackPatchRootDir.getAbsolutePath() + "/" + JPatchManager.PATCH_FILE_NAME);
				rollbackPatchFilesDir = new File(rollbackPatchRootDir.getAbsolutePath() + "/" + JPatchManager.PATCH_FILES_DIR_NAME);
				
				//if rollback is required, but cannot be prepared, doesnt make sense to go on (catching IOException at bottom)
				JPatchManager.prepareDir(rollbackPatchRootDir, true);
				JPatchManager.prepareDir(rollbackPatchFilesDir, true);
			}
			
			if(excludeMatcher == null)
			{
				//If no matcher provided, match everything 
//				excludeMatcher = Pattern.compile(".*").matcher("");
				excludeMatcher = Pattern.compile("a^").matcher("");
			}
			
			SAXReader saxReader = new SAXReader();
			Document patchdocument = 
					saxReader.read(
							new StringReader(
									FileUtils.readFileToString(patchFile)));

			if(patchdocument != null)
			{
	            List<File> addedRollbackFileList = new ArrayList<File>();
	            List<File> updatedRollbackFileList = new ArrayList<File>();
	            List<File> addedRollbackDirList = new ArrayList<File>(); 
	            List<File> removedRollbackFileList = new ArrayList<File>();
	            List<File> removedRollbackDirList = new ArrayList<File>();
				
				Element element = null;
				File targetFileOrDir = null;
				File patchedFileOrDir = null;
				String targetFileOrDirPath = null;
				
				if(!insertsOnly)
				{
					// ==================================================
					// DESTRUCTIVE OPERATIONS
					// ==================================================
					
					//delete removed directories
					Element removedDirectoriesElement = (Element)patchdocument.selectSingleNode(ELEMENT_PATCH + "/" + ELEMENT_REMOVED_DIRECTORIES);
					if(removedDirectoriesElement != null)
					{
		                for(Iterator<Element> elementIterator=removedDirectoriesElement.elementIterator(); elementIterator.hasNext(); )
		                {
		                    element = elementIterator.next();
		                    targetFileOrDirPath = convertPathToForwardSlash(targetRootDir.getAbsolutePath()) + "/" + element.attributeValue(ATTRIBUTE_PATH);
		                    if(targetFileOrDirPath != null && targetFileOrDirPath.length() > 0)
		                    {
			                    excludeMatcher.reset(targetFileOrDirPath);
			                    if(!excludeMatcher.matches())
			                    {
				                    targetFileOrDir = new File(targetFileOrDirPath);
				                    if(targetFileOrDir.exists())
				                    {
					                    if(rollbackPatchFilesDir != null)
					                    {
					                    	//copy removed directory to rollback location
					                    	FileUtils.copyDirectoryToDirectory(
					                    			targetFileOrDir, 
					                    			new File(rollbackPatchFilesDir.getAbsolutePath() + "/" + element.attributeValue(ATTRIBUTE_PATH)).getParentFile());
					                    	addedRollbackDirList.add(targetFileOrDir);
					                    }
					                    FileUtils.deleteDirectory(targetFileOrDir);
				                    }
			                    }
		                    }
		                }
					}
					
					//delete removed files
					Element removedFilesElement = (Element)patchdocument.selectSingleNode(ELEMENT_PATCH + "/" + ELEMENT_REMOVED_FILES);
					if(removedFilesElement != null)
					{
		                for(Iterator<Element> elementIterator=removedFilesElement.elementIterator(); elementIterator.hasNext(); )
		                {
		                    element = elementIterator.next();
		                    targetFileOrDirPath = convertPathToForwardSlash(targetRootDir.getAbsolutePath()) + "/" + element.attributeValue(ATTRIBUTE_PATH);
		                    if(targetFileOrDirPath != null && targetFileOrDirPath.length() > 0)
		                    {
			                    excludeMatcher.reset(targetFileOrDirPath);
			                    if(!excludeMatcher.matches())
			                    {
				                    targetFileOrDir = new File(targetFileOrDirPath);
				                    if(targetFileOrDir.exists())
				                    {
				                    	if(forceUpdates || targetFileOrDir.lastModified() == Long.valueOf(element.attributeValue(ATTRIBUTE_EXPECTED_LAST_MODIFIED)))
				                    	{
						                    if(rollbackPatchFilesDir != null)
						                    {
						                    	//copy removed directory to rollback location
						                    	FileUtils.copyFile(
						                    			targetFileOrDir, 
						                    			new File(rollbackPatchFilesDir.getAbsolutePath() + "/" + element.attributeValue(ATTRIBUTE_PATH)), 
						                    			true);
						                    	
						                    	addedRollbackFileList.add(targetFileOrDir);
						                    }
						                    FileUtils.deleteQuietly(targetFileOrDir);
					                    }
			                    	}				                    
			                    }
		                    }
		                }
					}
					
					
					//copy updated files
					Element updatedFilesElement = (Element)patchdocument.selectSingleNode(ELEMENT_PATCH + "/" + ELEMENT_UPDATED_FILES);
					if(updatedFilesElement != null)
					{
		                for(Iterator<Element> elementIterator=updatedFilesElement.elementIterator(); elementIterator.hasNext(); )
		                {
		                    element = elementIterator.next();
		                    targetFileOrDirPath = convertPathToForwardSlash(targetRootDir.getAbsolutePath()) + "/" + element.attributeValue(ATTRIBUTE_PATH);
		                    if(targetFileOrDirPath != null && targetFileOrDirPath.length() > 0)
		                    {
			                    excludeMatcher.reset(targetFileOrDirPath);
			                    if(!excludeMatcher.matches())
			                    {
				                    targetFileOrDir = new File(targetFileOrDirPath);
									patchedFileOrDir = new File(convertPathToForwardSlash(patchFilesDir.getAbsolutePath()) + "/" + element.attributeValue(ATTRIBUTE_PATH));
				                    if(targetFileOrDir.exists())
				                    {
				                    	if(forceUpdates || targetFileOrDir.lastModified() == Long.valueOf(element.attributeValue(ATTRIBUTE_EXPECTED_LAST_MODIFIED)))
				                    	{
				    	                    if(rollbackPatchFilesDir != null)
				    	                    {
				    	                    	//copy removed directory to rollback location
				    	                    	FileUtils.copyFile(
				    	                    			targetFileOrDir, 
				    	                    			new File(rollbackPatchFilesDir.getAbsolutePath() + "/" + element.attributeValue(ATTRIBUTE_PATH)), 
				    	                    			true);
				    	                    	
				    	                    	//this should be an updaterollback file list
				    	                    	updatedRollbackFileList.add(targetFileOrDir);
				    	                    }
				    	                    
				    	                    FileUtils.copyFileToDirectory(
				    	                    		patchedFileOrDir, 
				    	                    		targetFileOrDir.getParentFile(), 
				    	                    		true);
				                    	}
				                    }
				                    else
				                    {
				                    	//do add file
					                    if(rollbackPatchFilesDir != null)
					                    {
					                    	removedRollbackFileList.add(targetFileOrDir);
					                    }
					                    FileUtils.copyFileToDirectory(
					                    		patchedFileOrDir, 
					                    		targetFileOrDir.getParentFile(), 
					                    		true);
				                    }
			                    }
		                    }
		                }
					}
				}
				
				
				// ==================================================
				// NON DESTRUCTIVE OPERATIONS
				// ==================================================
				
				//create new directories
				Element addedDirectoriesElement = (Element)patchdocument.selectSingleNode(ELEMENT_PATCH + "/" + ELEMENT_ADDED_DIRECTORIES);
				if(addedDirectoriesElement != null)
				{
	                for(Iterator<Element> elementIterator=addedDirectoriesElement.elementIterator(); elementIterator.hasNext(); )
	                {
	                    element = elementIterator.next();
	                    targetFileOrDirPath = convertPathToForwardSlash(targetRootDir.getAbsolutePath()) + "/" + element.attributeValue(ATTRIBUTE_PATH);
	                    if(targetFileOrDirPath != null && targetFileOrDirPath.length() > 0)
	                    {
		                    excludeMatcher.reset(targetFileOrDirPath);
		                    if(!excludeMatcher.matches())
		                    {
			                    targetFileOrDir = new File(targetFileOrDirPath);
//			                    if(rollbackPatchFilesDir != null)
//			                    {
//			                    	removedRollbackDirList.add(targetFileOrDir);
//			                    }
			                    if(targetFileOrDir.exists())
			                    {
			                    	//if directory already exists, no need to do anything
			                    	if(forceUpdates)
			                    	{
			    	                    if(rollbackPatchFilesDir != null)
			    	                    {
			    	                    	//copy existing file to rollback location
					                    	FileUtils.copyDirectoryToDirectory(
					                    			targetFileOrDir, 
					                    			new File(rollbackPatchFilesDir.getAbsolutePath() + "/" + element.attributeValue(ATTRIBUTE_PATH)).getParentFile());
			    	                    	removedRollbackDirList.add(targetFileOrDir);
			    	                    }
					                    FileUtils.deleteQuietly(targetFileOrDir);
					                    FileUtils.forceMkdir(targetFileOrDir);
			                    	}
			                    	else
			                    	{
			                    		//if dir exists and force is false, do nothing
			                    	}
			                    }
			                    else
			                    {
				                    FileUtils.forceMkdir(targetFileOrDir);
			                    }
		                    }
	                    }
	                }
				}
				
				//create added files
				Element addedFilesElement = (Element)patchdocument.selectSingleNode(ELEMENT_PATCH + "/" + ELEMENT_ADDED_FILES);
				if(addedFilesElement != null)
				{
	                for(Iterator<Element> elementIterator=addedFilesElement.elementIterator(); elementIterator.hasNext(); )
	                {
	                    element = elementIterator.next();
	                    
	                    
	                    targetFileOrDirPath = convertPathToForwardSlash(targetRootDir.getAbsolutePath()) + "/" + element.attributeValue(ATTRIBUTE_PATH);
	                    if(targetFileOrDirPath != null && targetFileOrDirPath.length() > 0)
	                    {
		                    excludeMatcher.reset(targetFileOrDirPath);
		                    if(!excludeMatcher.matches())
		                    {
			                    targetFileOrDir = new File(targetFileOrDirPath);
			                    
								patchedFileOrDir = new File(convertPathToForwardSlash(patchFilesDir.getAbsolutePath()) + "/" + element.attributeValue(ATTRIBUTE_PATH));
			                    if(targetFileOrDir.exists())
			                    {
			                    	if(forceUpdates)
			                    	{
			    	                    if(rollbackPatchFilesDir != null)
			    	                    {
			    	                    	//copy existing file to rollback location
			    	                    	FileUtils.copyFile(
			    	                    			targetFileOrDir, 
			    	                    			new File(rollbackPatchFilesDir.getAbsolutePath() + "/" + element.attributeValue(ATTRIBUTE_PATH)), 
			    	                    			true);
			    	                    	updatedRollbackFileList.add(targetFileOrDir);
			    	                    }
			    	                    
			    	                    FileUtils.copyFileToDirectory(
			    	                    		patchedFileOrDir, 
			    	                    		targetFileOrDir.getParentFile(), 
			    	                    		true);
			                    	}
			                    }
			                    else
			                    {
				                    FileUtils.copyFileToDirectory(
				                    		patchedFileOrDir, 
				                    		targetFileOrDir.getParentFile(), 
				                    		true);
			                    }
		                    }
	                    }
	                }
				}
				
				// ==================================================
				// BUILD ROLLBACK PATCH
				//  ** rollback patches do NOT look at timestamps
				// ==================================================
				if(rollbackPatchFilesDir != null)
				{
					if(!rollbackPatchFilesDir.exists())
					{
						rollbackPatchFilesDir.mkdirs();
					}
					
					Document rollbackPatchDocument = DocumentHelper.createDocument();
					Element patchElement = rollbackPatchDocument.addElement(ELEMENT_PATCH);
					patchElement.addElement(ELEMENT_ADDED_FILES);
					patchElement.addElement(ELEMENT_UPDATED_FILES);
					patchElement.addElement(ELEMENT_REMOVED_FILES);
					patchElement.addElement(ELEMENT_ADDED_DIRECTORIES);
					patchElement.addElement(ELEMENT_REMOVED_DIRECTORIES);

		            for(File addedRollbackFile : addedRollbackFileList)
		            {
		            	JPatchManager.addPathElement(
		            			rollbackPatchDocument, 
		            			ELEMENT_PATCH + "/" + ELEMENT_ADDED_FILES, 
		            			ELEMENT_FILE, 
		            			ATTRIBUTE_PATH, 
		            			JPatchManager.getRelativePath(addedRollbackFile.getAbsolutePath(), targetRootDir.getAbsolutePath()));
		            }
					
		            for(File updatedRollbackFile : updatedRollbackFileList)
		            {
		            	JPatchManager.addPathElement(
		            			rollbackPatchDocument, 
		            			ELEMENT_PATCH + "/" + ELEMENT_ADDED_FILES, 
		            			ELEMENT_FILE, 
		            			ATTRIBUTE_PATH, 
		            			JPatchManager.getRelativePath(updatedRollbackFile.getAbsolutePath(), targetRootDir.getAbsolutePath()), 
		            			updatedRollbackFile.lastModified());
		            }
					
		            for(File addedRollbackDir : addedRollbackDirList)
		            {
		            	JPatchManager.addPathElement(
		            			rollbackPatchDocument, 
		            			ELEMENT_PATCH + "/" + ELEMENT_ADDED_DIRECTORIES, 
		            			ELEMENT_DIRECTORY, 
		            			ATTRIBUTE_PATH, 
		            			JPatchManager.getRelativePath(addedRollbackDir.getAbsolutePath(), targetRootDir.getAbsolutePath()));
		            }
					
		            for(File removedRollbackFile : removedRollbackFileList)
		            {
		            	JPatchManager.addPathElement(
		            			rollbackPatchDocument, 
		            			ELEMENT_PATCH + "/" + ELEMENT_REMOVED_FILES, 
		            			ELEMENT_FILE, 
		            			ATTRIBUTE_PATH, 
		            			JPatchManager.getRelativePath(removedRollbackFile.getAbsolutePath(), targetRootDir.getAbsolutePath()), 
		            			removedRollbackFile.lastModified());
		            }
		            
		            for(File removedRollbackDir : removedRollbackDirList)
		            {
		            	JPatchManager.addPathElement(
		            			rollbackPatchDocument, 
		            			ELEMENT_PATCH + "/" + ELEMENT_REMOVED_DIRECTORIES, 
		            			ELEMENT_DIRECTORY, 
		            			ATTRIBUTE_PATH, 
		            			JPatchManager.getRelativePath(removedRollbackDir.getAbsolutePath(), targetRootDir.getAbsolutePath()), 
		            			removedRollbackDir.lastModified());
		            }					
					
					JPatchManager.writeDocumentToFile(rollbackPatchDocument, rollbackPatchFile);
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	
	
	// ============================================================
	// UTILITY METHODS
	// ============================================================
	public class FileTuple
	{
		private File file;
		private long expectedLastModified;
		public FileTuple()
		{
			super();
		}
		public FileTuple(File file, long expectedLastModified)
		{
			super();
			this.file = file;
			this.expectedLastModified = expectedLastModified;
		}
		public File getFile()
		{
			return file;
		}
		public void setFile(File file) 
		{
			this.file = file;
		}
		public long getExpectedLastModified()
		{
			return expectedLastModified;
		}
		public void setExpectedLastModified(long expectedLastModified)
		{
			this.expectedLastModified = expectedLastModified;
		}
	}
	
	
	private static void prepareDir(File dir, boolean force) throws IOException
	{
		if(dir != null)
		{
			if(force && dir.exists())
			{
				FileUtils.forceDelete(dir);
			}
			FileUtils.forceMkdir(dir);
		}
	}
	
	
	private static boolean forceMakeDirectory(File directory)
	{
		boolean status = false;
		if(directory != null)
		{
			if(!directory.exists())
			{
				try
				{
					FileUtils.forceMkdir(directory);
					status = true;
				}
				catch(IOException ioe)
				{
					ioe.printStackTrace();
				}
			}
		}
		return status;
	}
	private static boolean forceCopyFileToDirectory(File file, File directory)
	{
		boolean status = false;
		if(file != null && directory != null)
		{
			if(!directory.exists())
			{
				try
				{
					FileUtils.forceMkdir(directory);
				}
				catch(IOException ioe)
				{
					ioe.printStackTrace();
				}
			}
			
			if(file.isFile())
			{
				try
				{
					FileUtils.copyFileToDirectory(file, directory, true);
					status = true;
				}
				catch(IOException ioe)
				{
					ioe.printStackTrace();
				}
			}
		}
		return status;
	}
	
	//TODO: better implementation
	public static String getRelativePath(String fullPath, String root)
	{
		String relativePath = "";
		if(fullPath != null)
		{
			if(root != null && fullPath.indexOf(root) == 0)
			{
				relativePath = fullPath.substring(root.length() + 1);
			}
		}
		return relativePath.replace("\\", "/");
	}
	
	private static Map<String, List<File>> categorizeFiles(File[] files)
	{
	    Map<String, List<File>> categorizedMap = null;
	    if(files != null)
	    {
	        categorizedMap = new HashMap<String, List<File>>();
            List<File> fileList = new ArrayList<File>();
            List<File> directoryList = new ArrayList<File>();
	        for(File file : files)
	        {
	            if(file.isDirectory())
	            {
	                directoryList.add(file);
	            }
	            else
	            {
	                fileList.add(file);
	            }
	        }
            categorizedMap.put(JPatchManager.FILE_LIST_KEY, fileList);
            categorizedMap.put(JPatchManager.DIR_LIST_KEY, directoryList);
	    }
	    return categorizedMap;
	}
	
	private static String convertPathToForwardSlash(String path)
	{
		if(path != null)
		{
			return path.replace("\\", "/");
		}
		return "";
	}

	private static String checksum(File file)
	{
	    String checksum = "";
	    if(file != null)
	    {
            FileInputStream fileInputStream = null;
	        try
	        {
	            fileInputStream = new FileInputStream(file);
	            checksum = org.apache.commons.codec.digest.DigestUtils.md5Hex(fileInputStream);
	        }
            catch(FileNotFoundException fnfe)
            {
                fnfe.printStackTrace();
            }
            catch(IOException ioe)
            {
                ioe.printStackTrace();
            }
	        finally
	        {
	        	if(fileInputStream != null)
	        	{
	        		try
	        		{
	        			fileInputStream.close();
	        		}
	        		catch(IOException ioe)
	        		{
	        			ioe.printStackTrace();
	        		}
	        	}
	        }
	    }
	    return checksum;
	}
	
	private static void writeDocumentToFile(Document document, File file)
	{
		if(document != null && file != null)
		{
			FileOutputStream fileOutputStream = null;
			try
			{
		        OutputFormat outformat = OutputFormat.createPrettyPrint();
		        outformat.setEncoding("UTF-8");
		        StringWriter stringWriter = new StringWriter();
		        XMLWriter xmlWriter = new XMLWriter(stringWriter, outformat);
		        xmlWriter.write(document);
//		        xmlWriter.flush();
		        logger.debug(stringWriter.toString());
		        
		        fileOutputStream = new FileOutputStream(file);
		        XMLWriter fileWriter = new XMLWriter(fileOutputStream, outformat);
		        fileWriter.write(document);
		        fileWriter.flush();
			}
			catch(IOException ioe)
			{
			    ioe.printStackTrace();
			}
			finally
			{
			    if(fileOutputStream != null)
			    {
			        try
			        {
			            fileOutputStream.close();
			        }
			        catch(Exception e)
			        {
			            e.printStackTrace();
			        }
			    }
			}
		}
	}
	
	private static Document addPathElement(Document document, String parentNodePath, String element, String attribute, String relativePath)
	{
		if(document != null)
		{
	        ((Element)document.selectSingleNode(parentNodePath)).addElement(element).addAttribute(attribute, relativePath);
		}
		return document;
	}
	private static Document addPathElement(Document document, String parentNodePath, String element, String attribute, String relativePath, long lastModifiedTimestamp)
	{
		if(document != null)
		{
	        ((Element)document.selectSingleNode(parentNodePath)).addElement(element).addAttribute(attribute, relativePath).addAttribute(ATTRIBUTE_EXPECTED_LAST_MODIFIED, String.valueOf(lastModifiedTimestamp));
		}
		return document;
	}
	
	
	
}

