package com.virtualparadigm.patch.cli;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import com.virtualparadigm.patch.processor.JPatchManager;

public class Main
{
	private static final String DEFAULT_PATCH_DIR_PATH = "patch";
	private static final String DEFAULT_TEMP_DIR_PATH = "temp";
	
	public static void main(String[] args)
	{
		if(args.length > 0)
		{
			CommandLineParser cliParser = new BasicParser();
			CommandLine cmd = null;
			try
			{
				cmd = cliParser.parse(Main.buildCommandLineOptions(args[0]), args);
			}
			catch(ParseException pe)
			{
				throw new RuntimeException(pe);
			}
			
	        if (cmd != null)
	        {
	            PatchCommand patchCommand = PatchCommand.get(args[0]);
	            if(patchCommand != null)
	            {
	            	try
	            	{
	                	switch(patchCommand)
	                	{
	        	    		case CREATE:
	        	    		{
	        					JPatchManager jpm = new JPatchManager();
	        					jpm.makePatch(
	        							new File(cmd.getOptionValue(PatchOption.OLD_DIR.getLongName())), 
	        							new File(cmd.getOptionValue(PatchOption.NEW_DIR.getLongName())), 
	        							Main.getOutputDirectory(cmd.getOptionValue(PatchOption.OUTPUT_DIR.getLongName())),
	        							cmd.getOptionValue(PatchOption.ARCHIVE_NAME.getLongName()));
	        	    			break;
	        	    		}
	        	    		case EXECUTE:
	        	    		{
	        					JPatchManager jpm = new JPatchManager();
	        					
	        					if(cmd.getOptionValue(PatchOption.ARCHIVE_PATH.getLongName()) == null || cmd.getOptionValue(PatchOption.ARCHIVE_PATH.getLongName()).length() == 0)
	        					{
	        						//no patch zip passed, so use patch root dir
	        						jpm.executePatch(
	        								new File(cmd.getOptionValue(PatchOption.PATCH_DIR.getLongName())), 
	        								new File(cmd.getOptionValue(PatchOption.TARGET_DIR.getLongName())), 
	        								Main.createMatcher(cmd.getOptionValue(PatchOption.EXCLUDE_REGEX.getLongName()), cmd.getOptionValue(PatchOption.EXCLUDE_GLOB.getLongName())), 
	        								(cmd.getOptionValue(PatchOption.ROLLBACK_DIR.getLongName()) == null) ? null : new File(cmd.getOptionValue(PatchOption.ROLLBACK_DIR.getLongName())), 
	        								Boolean.valueOf(cmd.getOptionValue(PatchOption.INSERTS_ONLY.getLongName())), 
	        								Boolean.valueOf(cmd.getOptionValue(PatchOption.FORCE_UPDATES.getLongName())));
	        					}
	        					else
	        					{
	        						if(cmd.getOptionValue(PatchOption.TEMP_DIR.getLongName()) != null && cmd.getOptionValue(PatchOption.TEMP_DIR.getLongName()).length() > 0)
	        						{
	        							jpm.executePatch(
	        									new File(cmd.getOptionValue(PatchOption.ARCHIVE_PATH.getLongName())), 
	    	        							Main.getTempDirectory(cmd.getOptionValue(PatchOption.TEMP_DIR.getLongName())),
	        									new File(cmd.getOptionValue(PatchOption.TARGET_DIR.getLongName())), 
	        									Main.createMatcher(cmd.getOptionValue(PatchOption.EXCLUDE_REGEX.getLongName()), cmd.getOptionValue(PatchOption.EXCLUDE_GLOB.getLongName())), 
	        									(cmd.getOptionValue(PatchOption.ROLLBACK_DIR.getLongName()) == null) ? null : new File(cmd.getOptionValue(PatchOption.ROLLBACK_DIR.getLongName())), 
	        									(cmd.hasOption((PatchOption.INSERTS_ONLY.getLongName())) ? true : false), 
	        									(cmd.hasOption((PatchOption.FORCE_UPDATES.getLongName())) ? true : false));
	        						}
	        						else
	        						{
	    	        	    			Main.printUnsupportedCommandError();
//	        							System.out.println("No temp directory specified. Please supply temp directory if executing a zipped patch");
	        						}
	        					}
	        	    			break;
	        	    		}
	        	    		default:
	        	    		{
	        	    			Main.printUnsupportedCommandError();
	        	    		}
	                	}
	                	
                	}
                	catch(Exception e)
                	{
                		e.printStackTrace();
                	}
	            }
                else
                {
                    Main.printUnsupportedCommandError();
                }
	        }
            else
            {
                Main.printUnsupportedCommandError();
            }
		}
        else
        {
            Main.printUnsupportedCommandError();
        }
		
	}
	
	
	
	private static File getOutputDirectory(String strOutputDir)
	{
		File outputDir = null;
		if(StringUtils.isEmpty(strOutputDir))
		{
			outputDir = new File(DEFAULT_PATCH_DIR_PATH);
		}
		else
		{
			outputDir = new File(strOutputDir);
		}
		return outputDir;
	}
	
	private static File getTempDirectory(String strTempDir)
	{
		File outputDir = null;
		if(StringUtils.isEmpty(strTempDir))
		{
			outputDir = new File(DEFAULT_TEMP_DIR_PATH);
		}
		else
		{
			outputDir = new File(strTempDir);
		}
		return outputDir;
	}
	
	
//	public static void callJPatchManager(CommandLine cmd, String operation)
//	{
//		
//		if(cmd != null)
//		{
//			if(CMD_CREATE.equalsIgnoreCase(operation))
//			{
//				JPatchManager jpm = new JPatchManager();
//				jpm.makePatch(
//						new File(cmd.getOptionValue(CMD_OPTION_LONG_OLD_STATE_DIR)), 
//						new File(cmd.getOptionValue(CMD_OPTION_LONG_NEW_STATE_DIR)), 
//						new File(cmd.getOptionValue(CMD_OPTION_LONG_OUTPUT_DIR)),
//						cmd.getOptionValue(CMD_OPTION_LONG_ARCHIVE_FILE_NAME));
//			}
//			else if(CMD_EXECUTE.equalsIgnoreCase(operation))
//			{
//				JPatchManager jpm = new JPatchManager();
//				
//				if(cmd.getOptionValue(CMD_OPTION_LONG_ARCHIVE_FILE_PATH) == null || cmd.getOptionValue(CMD_OPTION_LONG_ARCHIVE_FILE_PATH).length() == 0)
//				{
//					//no patch zip passed, so use patch root dir
//					jpm.executePatch(
//							new File(cmd.getOptionValue(CMD_OPTION_LONG_PATCH_DIR)), 
//							new File(cmd.getOptionValue(CMD_OPTION_LONG_TARGET_DIR)), 
//							Main.createMatcher(cmd.getOptionValue(CMD_OPTION_LONG_REGEX_EXCLUDE_FILTER), cmd.getOptionValue(CMD_OPTION_LONG_GLOB_EXCLUDE_FILTER)), 
////							Main.createFileFilter(cmd.getOptionValue(CMD_OPTION_LONG_REGEX_EXCLUDE_FILTER), cmd.getOptionValue(CMD_OPTION_LONG_GLOB_EXCLUDE_FILTER)), 
//							(cmd.getOptionValue(CMD_OPTION_LONG_ROLLBACK_DIR) == null) ? null : new File(cmd.getOptionValue(CMD_OPTION_LONG_ROLLBACK_DIR)), 
//							Boolean.valueOf(cmd.getOptionValue(CMD_OPTION_LONG_INSERT_ONLY)), 
//							Boolean.valueOf(cmd.getOptionValue(CMD_OPTION_LONG_FORCE_UPDATE)));
//				}
//				else
//				{
//					if(cmd.getOptionValue(CMD_OPTION_LONG_TEMP_DIR) != null && cmd.getOptionValue(CMD_OPTION_LONG_TEMP_DIR).length() > 0)
//					{
//						jpm.executePatch(
//								new File(cmd.getOptionValue(CMD_OPTION_LONG_ARCHIVE_FILE_PATH)), 
//								new File(cmd.getOptionValue(CMD_OPTION_LONG_TEMP_DIR)), 
//								new File(cmd.getOptionValue(CMD_OPTION_LONG_TARGET_DIR)), 
//								Main.createMatcher(cmd.getOptionValue(CMD_OPTION_LONG_REGEX_EXCLUDE_FILTER), cmd.getOptionValue(CMD_OPTION_LONG_GLOB_EXCLUDE_FILTER)), 
//								(cmd.getOptionValue(CMD_OPTION_LONG_ROLLBACK_DIR) == null) ? null : new File(cmd.getOptionValue(CMD_OPTION_LONG_ROLLBACK_DIR)), 
//								(cmd.hasOption((CMD_OPTION_LONG_INSERT_ONLY)) ? true : false), 
//								(cmd.hasOption((CMD_OPTION_LONG_FORCE_UPDATE)) ? true : false));
//					}
//					else
//					{
//						System.out.println("No temp directory specified. Please supply temp directory if executing a zipped patch");
//					}
//				}
//			}
//			else
//			{
//				System.out.println("unsupported JPatchManager operation");
//			}
//		}
//		else
//		{
//			System.out.println("No command specified. Options are:");
//			System.out.println("  " + CMD_CREATE);
//			System.out.println("  " + CMD_EXECUTE);
//		}
//	}
	
	
    private static void printUnsupportedCommandError()
    {
        System.out.println("Unsupported command.");
        Main.printHelp();
    }
	
    private static void printHelp()
    {
        System.out.println("Valid commands are:");
        System.out.println("  " + PatchCommand.CREATE.getName());
        System.out.println("    options:");
        System.out.println("      -" + PatchOption.OLD_DIR.getLongName());
        System.out.println("      -" + PatchOption.NEW_DIR.getLongName());
        System.out.println("      -" + PatchOption.OUTPUT_DIR.getLongName());
        System.out.println("      -" + PatchOption.ARCHIVE_PATH.getLongName() + " (optional)");
//        System.out.println("      -" + PatchOption.ARCHIVE_PATH.getLongName() + ", -" + PatchOption.TEMP_DIR.getLongName() + " (optional)");
//        System.out.println("      -" + PatchOption.TEMP_DIR.getLongName());
//        System.out.println("      -" + PatchOption.ARCHIVE_NAME.getLongName() + " (optional)");
        System.out.println("  " + PatchCommand.EXECUTE.getName());
        System.out.println("    options:");
        System.out.println("      -" + PatchOption.PATCH_DIR.getLongName());
        System.out.println("        OR");
        System.out.println("      -" + PatchOption.ARCHIVE_PATH.getLongName() + ", -" + PatchOption.TEMP_DIR.getLongName());
//        System.out.println("      -" + PatchOption.TEMP_DIR.getLongName());
        System.out.println("");
        System.out.println("      -" + PatchOption.TARGET_DIR.getLongName());
        System.out.println("      -" + PatchOption.EXCLUDE_REGEX.getLongName() + " (optional)");
        System.out.println("      -" + PatchOption.EXCLUDE_GLOB.getLongName() + " (optional)");
        System.out.println("      -" + PatchOption.ROLLBACK_DIR.getLongName() + " (optional)");
        System.out.println("      -" + PatchOption.INSERTS_ONLY.getLongName() + " (optional)");
        System.out.println("      -" + PatchOption.FORCE_UPDATES.getLongName() + " (optional)");
    }
	
	

	
	
	
    private static Options buildCommandLineOptions(String command)
    {
        Options cliOptions = null;
        PatchCommand patchCommand = PatchCommand.get(command);
        if(patchCommand != null)
        {
        	switch(patchCommand)
        	{
	    		case CREATE:
	    		{
	                cliOptions = Main.buildCreateCommandLineOptions();
	                break;
	    		}
	    		case EXECUTE:
	    		{
	                cliOptions = Main.buildExecuteCommandLineOptions();
	                break;
	    		}
	    		default:
	    		{
	                cliOptions = Main.buildCreateCommandLineOptions();
	    		}
        	}
        }
        else
        {
        	Main.printUnsupportedCommandError();
        }
        return cliOptions;
    }

    
    private static Options buildCreateCommandLineOptions()
    {
        Options cliOptions = new Options();
      
        OptionBuilder.withArgName(PatchOption.OLD_DIR.getShortName());
        OptionBuilder.withLongOpt(PatchOption.OLD_DIR.getLongName());
        OptionBuilder.withDescription(PatchOption.OLD_DIR.getDescription());
        OptionBuilder.hasArg(PatchOption.OLD_DIR.hasArgument());
        OptionBuilder.isRequired(true);
        cliOptions.addOption(OptionBuilder.create(PatchOption.OLD_DIR.getLongName()));        
        
        OptionBuilder.withArgName(PatchOption.NEW_DIR.getShortName());
        OptionBuilder.withLongOpt(PatchOption.NEW_DIR.getLongName());
        OptionBuilder.withDescription(PatchOption.NEW_DIR.getDescription());
        OptionBuilder.hasArg(PatchOption.NEW_DIR.hasArgument());
        OptionBuilder.isRequired(true);
        cliOptions.addOption(OptionBuilder.create(PatchOption.NEW_DIR.getLongName()));        
        
        OptionBuilder.withArgName(PatchOption.OUTPUT_DIR.getShortName());
        OptionBuilder.withLongOpt(PatchOption.OUTPUT_DIR.getLongName());
        OptionBuilder.withDescription(PatchOption.OUTPUT_DIR.getDescription());
        OptionBuilder.hasArg(PatchOption.OUTPUT_DIR.hasArgument());
        OptionBuilder.isRequired(true);
        cliOptions.addOption(OptionBuilder.create(PatchOption.OUTPUT_DIR.getLongName()));        
        
        OptionBuilder.withArgName(PatchOption.TEMP_DIR.getShortName());
        OptionBuilder.withLongOpt(PatchOption.TEMP_DIR.getLongName());
        OptionBuilder.withDescription(PatchOption.TEMP_DIR.getDescription());
        OptionBuilder.hasArg(PatchOption.TEMP_DIR.hasArgument());
        OptionBuilder.isRequired(false);
        cliOptions.addOption(OptionBuilder.create(PatchOption.TEMP_DIR.getLongName()));        
        
        OptionBuilder.withArgName(PatchOption.ARCHIVE_NAME.getShortName());
        OptionBuilder.withLongOpt(PatchOption.ARCHIVE_NAME.getLongName());
        OptionBuilder.withDescription(PatchOption.ARCHIVE_NAME.getDescription());
        OptionBuilder.hasArg(PatchOption.ARCHIVE_NAME.hasArgument());
        OptionBuilder.isRequired(false);
        cliOptions.addOption(OptionBuilder.create(PatchOption.ARCHIVE_NAME.getLongName()));        
        
        return cliOptions;
    }	
	
	
    private static Options buildExecuteCommandLineOptions()
    {
        Options cliOptions = new Options();
      
        //NEED EITHER PATCH DIR OR ARCHIVE,TEMP DIR
        OptionBuilder.withArgName(PatchOption.PATCH_DIR.getShortName());
        OptionBuilder.withLongOpt(PatchOption.PATCH_DIR.getLongName());
        OptionBuilder.withDescription(PatchOption.PATCH_DIR.getDescription());
        OptionBuilder.hasArg(PatchOption.PATCH_DIR.hasArgument());
        OptionBuilder.isRequired(false);
        cliOptions.addOption(OptionBuilder.create(PatchOption.PATCH_DIR.getLongName()));        
        
        OptionBuilder.withArgName(PatchOption.ARCHIVE_PATH.getShortName());
        OptionBuilder.withLongOpt(PatchOption.ARCHIVE_PATH.getLongName());
        OptionBuilder.withDescription(PatchOption.ARCHIVE_PATH.getDescription());
        OptionBuilder.hasArg(PatchOption.ARCHIVE_PATH.hasArgument());
        OptionBuilder.isRequired(false);
        cliOptions.addOption(OptionBuilder.create(PatchOption.ARCHIVE_PATH.getLongName()));        
        
        OptionBuilder.withArgName(PatchOption.TEMP_DIR.getShortName());
        OptionBuilder.withLongOpt(PatchOption.TEMP_DIR.getLongName());
        OptionBuilder.withDescription(PatchOption.TEMP_DIR.getDescription());
        OptionBuilder.hasArg(PatchOption.TEMP_DIR.hasArgument());
        OptionBuilder.isRequired(false);
        cliOptions.addOption(OptionBuilder.create(PatchOption.TEMP_DIR.getLongName()));        
        
        OptionBuilder.withArgName(PatchOption.TARGET_DIR.getShortName());
        OptionBuilder.withLongOpt(PatchOption.TARGET_DIR.getLongName());
        OptionBuilder.withDescription(PatchOption.TARGET_DIR.getDescription());
        OptionBuilder.hasArg(PatchOption.TARGET_DIR.hasArgument());
        OptionBuilder.isRequired(true);
        cliOptions.addOption(OptionBuilder.create(PatchOption.TARGET_DIR.getLongName()));        
        
        OptionBuilder.withArgName(PatchOption.EXCLUDE_REGEX.getShortName());
        OptionBuilder.withLongOpt(PatchOption.EXCLUDE_REGEX.getLongName());
        OptionBuilder.withDescription(PatchOption.EXCLUDE_REGEX.getDescription());
        OptionBuilder.hasArg(PatchOption.EXCLUDE_REGEX.hasArgument());
        OptionBuilder.isRequired(false);
        cliOptions.addOption(OptionBuilder.create(PatchOption.EXCLUDE_REGEX.getLongName()));        
        
        OptionBuilder.withArgName(PatchOption.EXCLUDE_GLOB.getShortName());
        OptionBuilder.withLongOpt(PatchOption.EXCLUDE_GLOB.getLongName());
        OptionBuilder.withDescription(PatchOption.EXCLUDE_GLOB.getDescription());
        OptionBuilder.hasArg(PatchOption.EXCLUDE_GLOB.hasArgument());
        OptionBuilder.isRequired(false);
        cliOptions.addOption(OptionBuilder.create(PatchOption.EXCLUDE_GLOB.getLongName()));        
        
        OptionBuilder.withArgName(PatchOption.ROLLBACK_DIR.getShortName());
        OptionBuilder.withLongOpt(PatchOption.ROLLBACK_DIR.getLongName());
        OptionBuilder.withDescription(PatchOption.ROLLBACK_DIR.getDescription());
        OptionBuilder.hasArg(PatchOption.ROLLBACK_DIR.hasArgument());
        OptionBuilder.isRequired(false);
        cliOptions.addOption(OptionBuilder.create(PatchOption.ROLLBACK_DIR.getLongName()));        
        
        OptionBuilder.withArgName(PatchOption.INSERTS_ONLY.getShortName());
        OptionBuilder.withLongOpt(PatchOption.INSERTS_ONLY.getLongName());
        OptionBuilder.withDescription(PatchOption.INSERTS_ONLY.getDescription());
        OptionBuilder.hasArg(PatchOption.INSERTS_ONLY.hasArgument());
        OptionBuilder.isRequired(false);
        cliOptions.addOption(OptionBuilder.create(PatchOption.INSERTS_ONLY.getLongName()));        
        
        OptionBuilder.withArgName(PatchOption.FORCE_UPDATES.getShortName());
        OptionBuilder.withLongOpt(PatchOption.FORCE_UPDATES.getLongName());
        OptionBuilder.withDescription(PatchOption.FORCE_UPDATES.getDescription());
        OptionBuilder.hasArg(PatchOption.FORCE_UPDATES.hasArgument());
        OptionBuilder.isRequired(false);
        cliOptions.addOption(OptionBuilder.create(PatchOption.FORCE_UPDATES.getLongName()));        
        
        return cliOptions;
    }
    
    
    // =====================================================
    // UTILITY METHODS
    // =====================================================
	private static Matcher createMatcher(String regexFilterExpression, String globFilterExpression)
	{
		Matcher matcher = null;
		if(regexFilterExpression != null && regexFilterExpression.length() > 0)
		{
			matcher = Pattern.compile(regexFilterExpression).matcher("");
		}
		else if(globFilterExpression != null && globFilterExpression.length() > 0)
		{
			matcher = Pattern.compile(Main.convertGlobToRegex(globFilterExpression)).matcher("");
		}
		return matcher;
	}
	
	private static String convertGlobToRegex(String globExpression)
	{
		String regex = "";
		if(globExpression != null)
		{
			
		}
		return regex;
	}
//	private static FileFilter createFileFilter(String regexFilterExpression, String wildcardFilterExpression)
//	{
//		FileFilter fileFilter = null;
//		if(regexFilterExpression != null && regexFilterExpression.length() > 0)
//		{
//			fileFilter = new RegexFileFilter(regexFilterExpression);
//		}
//		else if(wildcardFilterExpression != null && wildcardFilterExpression.length() > 0)
//		{
//			fileFilter = new WildcardFileFilter(wildcardFilterExpression);
//		}
//		return fileFilter;
//	}
		
}