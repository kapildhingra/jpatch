package com.virtualparadigm.patch.cli;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum PatchOption
{
    OLD_DIR("od", "oldDir", "old version directory", true), 
    NEW_DIR("nd", "newDir", "new version directory", true),
    TARGET_DIR("td", "targetDir", "target directory", true), 
    EXCLUDE_REGEX("er", "excludeRegex", "regex pattern for exclude", true), 
    EXCLUDE_GLOB("eg", "excludeGlob", "glob pattern for exclude", true), 
    OUTPUT_DIR("otd", "outputDir", "output directory", true),
    TEMP_DIR("td", "tempDir", "temporary directory", true),
    ARCHIVE_NAME("an", "archiveName", "name of archive file", true),
    ARCHIVE_PATH("ap", "archivePath", "archive file path", true),
    PATCH_DIR("pd", "patchDir", "patch directory", true),
    ROLLBACK_DIR("rd", "rollbackDir", "rollback patch directory", true),
    INSERTS_ONLY("i", "insertsOnly", "inserts only", true),
    FORCE_UPDATES("f", "forceUpdates", "force updates", true);

    private static final Map<String, PatchOption> lookupMap = new HashMap<String, PatchOption>();
    static
    {
    	for(PatchOption ao : EnumSet.allOf(PatchOption.class))
    	{
    		lookupMap.put(ao.getLongName(), ao);
    	}
    }
    
    private PatchOption(String shortName, String longName, String description, boolean argument)
    {
        this.shortName = shortName;
        this.longName = longName;
        this.argument = argument;
    }
    
    private String shortName;
    private String longName;
    private String description;
    private boolean argument;
    
    public String getShortName()
    {
        return shortName;
    }
    
    public String getLongName()
    {
        return longName;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public boolean hasArgument()
    {
        return argument;
    }
    
    public static PatchOption get(String strAuthoringOption)
    {
    	return lookupMap.get(strAuthoringOption);
    }
}

