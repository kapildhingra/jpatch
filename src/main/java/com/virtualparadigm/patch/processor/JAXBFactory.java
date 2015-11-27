package com.virtualparadigm.patch.processor;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import com.virtualparadigm.patch.xml.Patch;

public class JAXBFactory
{
    private static JAXBContext jaxbContext = null;
    
    static
    {
    	JAXBFactory.jaxbContext = null;
        try
        {
        	JAXBFactory.jaxbContext = JAXBContext.newInstance(Patch.class);
        }
        catch(JAXBException jbe)
        {
        	jbe.printStackTrace();
        }
    }
    
    public static Marshaller createJAXBPatchMarshaller()
    {
    	return JAXBFactory.createJAXBMarshaller();
    }
    public static Unmarshaller createJAXBPatchUnmarshaller()
    {
    	return JAXBFactory.createJAXBUnmarshaller();
    }
    
    
    private static Marshaller createJAXBMarshaller()
    {
    	Marshaller marshaller = null;
    	if(JAXBFactory.jaxbContext != null)
    	{
            try
            {
            	marshaller = JAXBFactory.jaxbContext.createMarshaller();
            	marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            }
            catch(Exception e)
            {
            	e.printStackTrace();
            }
    	}
        return marshaller;
    }
    
    public static Unmarshaller createJAXBUnmarshaller()
    {
    	Unmarshaller unmarshaller = null;
    	if(JAXBFactory.jaxbContext != null)
    	{
            try
            {
            	unmarshaller = JAXBFactory.jaxbContext.createUnmarshaller();
//            	URL xsdFileURL = ConfigurationLoader.class.getClassLoader().getResource(xsdFileName);
//            	unmarshaller.setSchema(SchemaFactory.newInstance(SCHEMA_LANGUAGE).newSchema(xsdFileURL));
            }
            catch(Exception e)
            {
            	e.printStackTrace();
            }
    	}
        return unmarshaller;
    }
    
}