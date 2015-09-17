package com.virtualparadigm.patch.cli;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum PatchCommand
{
    CREATE("create", "create patch"),
    EXECUTE("execute", "execute patch");
    
    private static final Map<String, PatchCommand> lookupMap = new HashMap<String, PatchCommand>();
    static
    {
    	for(PatchCommand ac : EnumSet.allOf(PatchCommand.class))
    	{
    		lookupMap.put(ac.getName(), ac);
    	}
    }
    
    private PatchCommand(String name, String description)
    {
        this.name = name;
        this.description = description;
    }
    
    private String name;
    private String description;
    
    public String getName()
    {
        return name;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    public static PatchCommand get(String strPatchCommand)
    {
    	return lookupMap.get(strPatchCommand);
    }
}
