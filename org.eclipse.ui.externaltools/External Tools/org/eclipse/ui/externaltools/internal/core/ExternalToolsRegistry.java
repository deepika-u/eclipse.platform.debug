package org.eclipse.ui.externaltools.internal.core;

/**********************************************************************
Copyright (c) 2002 IBM Corp. and others.
All rights reserved.   This program and the accompanying materials
are made available under the terms of the Common Public License v0.5
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v05.html
 
Contributors:
**********************************************************************/
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.XMLMemento;

/**
 * The registry of user defined external tools that can
 * be run using the external tool menu. It does not include
 * any external tools that are assigned as builders on a
 * project.
 */
public final class ExternalToolsRegistry {
	private static final String STATE_FILE_NAME = "externaltools.xml"; //$NON-NLS-1$
	private static final String TAG_EXTERNALTOOLS = "externaltools"; //$NON-NLS-1$
	private static final String TAG_TOOL = "tool"; //$NON-NLS-1$
	private static final String TAG_ENTRY = "entry"; //$NON-NLS-1$
	private static final String TAG_KEY = "key"; //$NON-NLS-1$
	private static final String TAG_VALUE = "value"; //$NON-NLS-1$
	
	private ArrayList externalTools;

	/**
	 * Creates the registry and loads the saved
	 * external tools.
	 */
	/*package*/ ExternalToolsRegistry() {
		super();
		loadExternalTools();
	}

	/**
	 * Returns the named external tool or <code>null</code>
	 * if not found.
	 */
	public ExternalTool getExternalTool(String name) {
		Iterator enum = externalTools.iterator();
		while (enum.hasNext()) {
			ExternalTool script = (ExternalTool)enum.next();
			if (script.getName().equals(name))
				return script;
		}
		return null;
	}
	
	/**
	 * Returns the external tools of the registry
	 */
	public ArrayList getExternalTools() {
		return externalTools;
	}
	
	/**
	 * Sets the external tools for the registry.
	 * Causes them to be saved to disk.
	 */
	public boolean setExternalTools(ArrayList scripts) {
		this.externalTools = scripts;
		return saveExternalTools();
	}
	
	/**
	 * Loads the external tools from storage and
	 * adds them to the registry.
	 */
	private void loadExternalTools() {
		IPath path = ExternalToolsPlugin.getDefault().getStateLocation();
		path = path.append(STATE_FILE_NAME);
		InputStreamReader reader = null;
		try {
			FileInputStream input = new FileInputStream(path.toFile());
			reader = new InputStreamReader(input, "utf-8"); //$NON-NLS-1$
			XMLMemento memento = XMLMemento.createReadRoot(reader);
			
			// Get the tool script children element
			IMemento[] scripts = memento.getChildren(TAG_TOOL);
			externalTools = new ArrayList(scripts.length);
			for (int i = 0; i < scripts.length; i++) {
				HashMap args = new HashMap();
				IMemento[] entries = scripts[i].getChildren(TAG_ENTRY);
				for (int j = 0; j < entries.length; j++) {
					String key = entries[j].getString(TAG_KEY);
					if (key != null) {
						String value = entries[j].getTextData();
						args.put(key, value);
					}
				}
				ExternalTool script = ExternalTool.fromArgumentMap(args);
				if (script != null)
					externalTools.add(script);
			}
		}
		catch (FileNotFoundException e) {
			// Silently ignore this...
		}
		catch (IOException e) {
			ExternalToolsPlugin.getDefault().log("File I/O error with external tool state reader.", e); //$NON-NLS-1$
		}
		finally {
			if (reader != null) {
				try {
					reader.close();
				} catch(IOException e) {
					ExternalToolsPlugin.getDefault().log("Unable to close external tool state reader.", e); //$NON-NLS-1$
				}
			}
			if (externalTools == null)
				externalTools = new ArrayList(0);
		}
	}
	
	/**
	 * Saves the external tool to storage.
	 * 
	 * @return true if save is successful, false otherwise.
	 */
	/*package*/ boolean saveExternalTools() {
		boolean successful = true;
		
		// Populate the memento
		XMLMemento memento = XMLMemento.createWriteRoot(TAG_EXTERNALTOOLS);
		Iterator enum = externalTools.iterator();
		while (enum.hasNext()) {
			IMemento scriptMemento = memento.createChild(TAG_TOOL);
			ExternalTool script = (ExternalTool)enum.next();
			Map args = script.toArgumentMap();
			Iterator entries = args.entrySet().iterator();
			while (entries.hasNext()) {
				Map.Entry entry = (Map.Entry)entries.next();
				IMemento entryMemento = scriptMemento.createChild(TAG_ENTRY);
				entryMemento.putString(TAG_KEY, (String)entry.getKey());
				entryMemento.putTextData((String)entry.getValue());
			}
		}

		// Write the memento to the state file		
		IPath path = ExternalToolsPlugin.getDefault().getStateLocation();
		path = path.append(STATE_FILE_NAME);
		File stateFile = path.toFile();
		try {
			FileOutputStream stream = new FileOutputStream(stateFile);
			OutputStreamWriter writer = new OutputStreamWriter(stream, "utf-8"); //$NON-NLS-1$
			memento.save(writer);
			writer.close();
		} catch (IOException e) {
			stateFile.delete();
			MessageDialog.openError(
				null,
				ToolMessages.getString("ExternalToolsRegistry.saveStateErrorTitle"), //$NON-NLS-1$
				ToolMessages.getString("ExternalToolsRegistry.saveStateError")); //$NON-NLS-1$
			successful = false;
		}
		
		return successful;
	}
}
