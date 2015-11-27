package com.virtualparadigm.patch.processor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.virtualparadigm.patch.util.ZipUtils;
import com.virtualparadigm.patch.xml.AddedDirectories;
import com.virtualparadigm.patch.xml.AddedFiles;
import com.virtualparadigm.patch.xml.Patch;
import com.virtualparadigm.patch.xml.RemovedDirectories;
import com.virtualparadigm.patch.xml.RemovedFiles;
import com.virtualparadigm.patch.xml.Resource;
import com.virtualparadigm.patch.xml.TimestampedResource;
import com.virtualparadigm.patch.xml.UpdatedFiles;

public class JPatchManager
{
	private static Logger logger = LoggerFactory.getLogger(JPatchManager.class);
	
    private static final String PATCH_FILE_NAME = "patch.xml";
    private static final String PATCH_FILES_DIR_NAME = "patch-files";
    private static final String FILE_LIST_KEY = "fileList";
    private static final String DIR_LIST_KEY = "directoryList";
    
    private static Marshaller patchJAXBMarshaller = null;
    private static Unmarshaller patchJAXBUnmarshaller = null;

    static
    {
    	patchJAXBMarshaller = JAXBFactory.createJAXBPatchMarshaller();
    	patchJAXBUnmarshaller = JAXBFactory.createJAXBPatchUnmarshaller();
    }    
	
	
	public JPatchManager()
	{
		super(); 
	}
	
	public static void main(String[] args) throws IOException
	{
		
		
	}

	public Patch makePatch(File oldStateRootDir, File newStateRootDir, File outputDir, String archiveFileName)// throws IOException
	{
		Patch patch = new Patch();
		
		try
		{
			//if prepare fails (IOException), doesn't make sense to go further
	        JPatchManager.prepareDir(outputDir, false);
			File patchFile = new File(outputDir.getAbsolutePath() + "/" + JPatchManager.PATCH_FILE_NAME);
			File patchFilesDir = new File(outputDir.getAbsolutePath() + "/" + JPatchManager.PATCH_FILES_DIR_NAME);
			
			patch = this.createDirectoryPatch(oldStateRootDir, "/", newStateRootDir, "/", patchFilesDir, patch);
			JPatchManager.patchJAXBMarshaller.marshal(patch, patchFile);
			
			if(archiveFileName != null && archiveFileName.length() > 0)
			{
				ZipUtils.createZipFile(outputDir.getAbsolutePath() + "/" + archiveFileName, new File[]{patchFile, patchFilesDir}, 1024);
				FileUtils.deleteQuietly(patchFile);
				FileUtils.deleteQuietly(patchFilesDir);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		return patch;
	}
	
	
	private Patch createDirectoryPatch(File oldStateRootDir, String oldStateRelDirPath, File newStateRootDir, String newStateRelDirPath, File patchFilesRootDir, Patch patch)
	{
		if(newStateRelDirPath != null && patch != null)
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
                                    patch);
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
                                        patch);
                            }
                            else
                            {
                                this.createDirectoryPatch(
                                        oldStateRootDir, 
                                        oldStateRelDirPath + File.separator + foundOldStateDir.getName(), 
                                        newStateRootDir, 
                                        newStateRelDirPath + File.separator + newStateSubDirList.get(newStateSubDirIndex).getName(), 
                                        patchFilesRootDir, 
                                        patch);
                            
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
            	JPatchManager.addAddedDirectory(
            			JPatchManager.getRelativePath(addedDir.getAbsolutePath(), patchFilesRootDir.getAbsolutePath()) + "/", 
            			patch);
            }
			
            for(File removedDir : removedDirList)
            {
            	JPatchManager.addRemovedDirectory(
            			JPatchManager.getRelativePath(removedDir.getAbsolutePath(), oldStateRootDir.getAbsolutePath()) + "/", 
            			removedDir.lastModified(), 
            			patch);
            }
			
            for(File addedFile : addedFileList)
            {
            	JPatchManager.addAddedFile(
            			JPatchManager.getRelativePath(addedFile.getAbsolutePath(), patchFilesRootDir.getAbsolutePath()) + "/", 
            			patch);
            }
			
            for(FileTuple updatedFileTuple : updatedFileTupleList)
            {
            	JPatchManager.addUpdatedFile(
            			JPatchManager.getRelativePath(updatedFileTuple.getFile().getAbsolutePath(), patchFilesRootDir.getAbsolutePath()) + "/", 
            			updatedFileTuple.getExpectedLastModified(), 
            			patch);
            }
            
            for(File removedFile : removedFileList)
            {
            	JPatchManager.addRemovedFile(
            			JPatchManager.getRelativePath(removedFile.getAbsolutePath(), oldStateRootDir.getAbsolutePath()) + "/", 
            			removedFile.lastModified(), 
            			patch);
            }
		}
		return patch;
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
			
			Patch patch = (Patch)JPatchManager.patchJAXBUnmarshaller.unmarshal(patchFile);

			if(patch != null)
			{
	            List<File> addedRollbackFileList = new ArrayList<File>();
	            List<File> updatedRollbackFileList = new ArrayList<File>();
	            List<File> addedRollbackDirList = new ArrayList<File>(); 
	            List<File> removedRollbackFileList = new ArrayList<File>();
	            List<File> removedRollbackDirList = new ArrayList<File>();
				
				File targetFileOrDir = null;
				File patchedFileOrDir = null;
				String targetFileOrDirPath = null;
				
				if(!insertsOnly)
				{
					// ==================================================
					// DESTRUCTIVE OPERATIONS
					// ==================================================
					
					//delete removed directories
					if(patch.getRemovedDirectories() != null)
					{
						for(TimestampedResource removedDirectory : patch.getRemovedDirectories().getDirectories())
						{
		                    targetFileOrDirPath = convertPathToForwardSlash(targetRootDir.getAbsolutePath()) + "/" + removedDirectory.getPath();
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
					                    			new File(rollbackPatchFilesDir.getAbsolutePath() + "/" + removedDirectory.getPath()).getParentFile());
					                    	addedRollbackDirList.add(targetFileOrDir);
					                    }
					                    FileUtils.deleteDirectory(targetFileOrDir);
				                    }
			                    }
		                    }
						}
					}
					
					
					//delete removed files
					if(patch.getRemovedFiles() != null)
					{
						for(TimestampedResource removedFile : patch.getRemovedFiles().getFiles())
						{
		                    targetFileOrDirPath = convertPathToForwardSlash(targetRootDir.getAbsolutePath()) + "/" + removedFile.getPath();
		                    if(targetFileOrDirPath != null && targetFileOrDirPath.length() > 0)
		                    {
			                    excludeMatcher.reset(targetFileOrDirPath);
			                    if(!excludeMatcher.matches())
			                    {
				                    targetFileOrDir = new File(targetFileOrDirPath);
				                    if(targetFileOrDir.exists())
				                    {
				                    	if(forceUpdates || targetFileOrDir.lastModified() == removedFile.getExpectedLastModified())
				                    	{
						                    if(rollbackPatchFilesDir != null)
						                    {
						                    	//copy removed directory to rollback location
						                    	FileUtils.copyFile(
						                    			targetFileOrDir, 
						                    			new File(rollbackPatchFilesDir.getAbsolutePath() + "/" + removedFile.getPath()), 
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
					if(patch.getUpdatedFiles() != null)
					{
						for(TimestampedResource updatedFile : patch.getUpdatedFiles().getFiles())
						{
		                    targetFileOrDirPath = convertPathToForwardSlash(targetRootDir.getAbsolutePath()) + "/" + updatedFile.getPath();
		                    if(targetFileOrDirPath != null && targetFileOrDirPath.length() > 0)
		                    {
			                    excludeMatcher.reset(targetFileOrDirPath);
			                    if(!excludeMatcher.matches())
			                    {
				                    targetFileOrDir = new File(targetFileOrDirPath);
									patchedFileOrDir = new File(convertPathToForwardSlash(patchFilesDir.getAbsolutePath()) + "/" + updatedFile.getPath());
				                    if(targetFileOrDir.exists())
				                    {
				                    	if(forceUpdates || targetFileOrDir.lastModified() == Long.valueOf(updatedFile.getExpectedLastModified()))
				                    	{
				    	                    if(rollbackPatchFilesDir != null)
				    	                    {
				    	                    	//copy removed directory to rollback location
				    	                    	FileUtils.copyFile(
				    	                    			targetFileOrDir, 
				    	                    			new File(rollbackPatchFilesDir.getAbsolutePath() + "/" + updatedFile.getPath()), 
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
				if(patch.getAddedDirectories() != null)
				{
					for(Resource addedDirectory : patch.getAddedDirectories().getDirectories())
					{
	                    targetFileOrDirPath = convertPathToForwardSlash(targetRootDir.getAbsolutePath()) + "/" + addedDirectory.getPath();
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
					                    			new File(rollbackPatchFilesDir.getAbsolutePath() + "/" + addedDirectory.getPath()).getParentFile());
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
				if(patch.getAddedFiles() != null)
				{
					for(Resource addedFile : patch.getAddedFiles().getFiles())
					{
	                    targetFileOrDirPath = convertPathToForwardSlash(targetRootDir.getAbsolutePath()) + "/" + addedFile.getPath();
	                    if(targetFileOrDirPath != null && targetFileOrDirPath.length() > 0)
	                    {
		                    excludeMatcher.reset(targetFileOrDirPath);
		                    if(!excludeMatcher.matches())
		                    {
			                    targetFileOrDir = new File(targetFileOrDirPath);
			                    
								patchedFileOrDir = new File(convertPathToForwardSlash(patchFilesDir.getAbsolutePath()) + "/" + addedFile.getPath());
			                    if(targetFileOrDir.exists())
			                    {
			                    	if(forceUpdates)
			                    	{
			    	                    if(rollbackPatchFilesDir != null)
			    	                    {
			    	                    	//copy existing file to rollback location
			    	                    	FileUtils.copyFile(
			    	                    			targetFileOrDir, 
			    	                    			new File(rollbackPatchFilesDir.getAbsolutePath() + "/" + addedFile.getPath()), 
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
					
					Patch rollbackPatch = new Patch();

		            for(File addedRollbackFile : addedRollbackFileList)
		            {
		            	JPatchManager.addAddedFile(
		            			JPatchManager.getRelativePath(addedRollbackFile.getAbsolutePath(), targetRootDir.getAbsolutePath()), 
		            			rollbackPatch);
		            }
					
		            for(File updatedRollbackFile : updatedRollbackFileList)
		            {
		            	JPatchManager.addUpdatedFile(
		            			JPatchManager.getRelativePath(updatedRollbackFile.getAbsolutePath(), targetRootDir.getAbsolutePath()), 
		            			updatedRollbackFile.lastModified(),
		            			rollbackPatch);
		            }
					
		            for(File addedRollbackDir : addedRollbackDirList)
		            {
		            	JPatchManager.addAddedDirectory(
		            			JPatchManager.getRelativePath(addedRollbackDir.getAbsolutePath(), targetRootDir.getAbsolutePath()),
		            			rollbackPatch);
		            }
					
		            for(File removedRollbackFile : removedRollbackFileList)
		            {
		            	JPatchManager.addRemovedFile(
		            			JPatchManager.getRelativePath(removedRollbackFile.getAbsolutePath(), targetRootDir.getAbsolutePath()), 
		            			removedRollbackFile.lastModified(),
		            			rollbackPatch);
		            }
		            
		            for(File removedRollbackDir : removedRollbackDirList)
		            {
		            	JPatchManager.addRemovedDirectory(
		            			JPatchManager.getRelativePath(removedRollbackDir.getAbsolutePath(), targetRootDir.getAbsolutePath()), 
		            			removedRollbackDir.lastModified(),
		            			rollbackPatch);
		            }					
					JPatchManager.patchJAXBMarshaller.marshal(rollbackPatch, rollbackPatchFile);
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
	
	
//	private static void writeDocumentToFile(Document document, File file)
//	{
//		if(document != null && file != null)
//		{
//			FileOutputStream fileOutputStream = null;
//			try
//			{
//		        OutputFormat outformat = OutputFormat.createPrettyPrint();
//		        outformat.setEncoding("UTF-8");
//		        StringWriter stringWriter = new StringWriter();
//		        XMLWriter xmlWriter = new XMLWriter(stringWriter, outformat);
//		        xmlWriter.write(document);
////		        xmlWriter.flush();
//		        logger.debug(stringWriter.toString());
//		        
//		        fileOutputStream = new FileOutputStream(file);
//		        XMLWriter fileWriter = new XMLWriter(fileOutputStream, outformat);
//		        fileWriter.write(document);
//		        fileWriter.flush();
//			}
//			catch(IOException ioe)
//			{
//			    ioe.printStackTrace();
//			}
//			finally
//			{
//			    if(fileOutputStream != null)
//			    {
//			        try
//			        {
//			            fileOutputStream.close();
//			        }
//			        catch(Exception e)
//			        {
//			            e.printStackTrace();
//			        }
//			    }
//			}
//		}
//	}
	
	
	private static Patch addAddedFile(String relativePath, Patch patch)
	{
		if(StringUtils.isNotEmpty(relativePath) && patch != null)
		{
			Resource resource = new Resource();
			resource.setPath(relativePath);
			if(patch.getAddedFiles() == null)
			{
				patch.setAddedFiles(new AddedFiles());
			}
			patch.getAddedFiles().getFiles().add(resource);
		}
		return patch;
	}
	
	private static Patch addUpdatedFile(String relativePath, long expectedLastModified, Patch patch)
	{
		if(StringUtils.isNotEmpty(relativePath) && patch != null)
		{
			TimestampedResource resource = new TimestampedResource();
			resource.setPath(relativePath);
			resource.setExpectedLastModified(expectedLastModified);
			if(patch.getUpdatedFiles() == null)
			{
				patch.setUpdatedFiles(new UpdatedFiles());
			}
			patch.getUpdatedFiles().getFiles().add(resource);
		}
		return patch;
	}
	
	private static Patch addRemovedFile(String relativePath, long expectedLastModified, Patch patch)
	{
		if(StringUtils.isNotEmpty(relativePath) && patch != null)
		{
			TimestampedResource resource = new TimestampedResource();
			resource.setPath(relativePath);
			resource.setExpectedLastModified(expectedLastModified);
			if(patch.getRemovedFiles() == null)
			{
				patch.setRemovedFiles(new RemovedFiles());
			}
			patch.getRemovedFiles().getFiles().add(resource);
		}
		return patch;
	}
	
	private static Patch addAddedDirectory(String relativePath, Patch patch)
	{
		if(StringUtils.isNotEmpty(relativePath) && patch != null)
		{
			Resource resource = new Resource();
			resource.setPath(relativePath);
			if(patch.getAddedDirectories() == null)
			{
				patch.setAddedDirectories(new AddedDirectories());
			}
			patch.getAddedDirectories().getDirectories().add(resource);
		}
		return patch;
	}
	
	private static Patch addRemovedDirectory(String relativePath, long expectedLastModified, Patch patch)
	{
		if(StringUtils.isNotEmpty(relativePath) && patch != null)
		{
			TimestampedResource resource = new TimestampedResource();
			resource.setPath(relativePath);
			resource.setExpectedLastModified(expectedLastModified);
			if(patch.getRemovedDirectories() == null)
			{
				patch.setRemovedDirectories(new RemovedDirectories());
			}
			patch.getRemovedDirectories().getDirectories().add(resource);
		}
		return patch;
	}
	
	
//	private static Document addPathElement(Document document, String parentNodePath, String element, String attribute, String relativePath)
//	{
//		if(document != null)
//		{
//	        ((Element)document.selectSingleNode(parentNodePath)).addElement(element).addAttribute(attribute, relativePath);
//		}
//		return document;
//	}
//	private static Document addPathElement(Document document, String parentNodePath, String element, String attribute, String relativePath, long lastModifiedTimestamp)
//	{
//		if(document != null)
//		{
//	        ((Element)document.selectSingleNode(parentNodePath)).addElement(element).addAttribute(attribute, relativePath).addAttribute(ATTRIBUTE_EXPECTED_LAST_MODIFIED, String.valueOf(lastModifiedTimestamp));
//		}
//		return document;
//	}
	
	
	
}

