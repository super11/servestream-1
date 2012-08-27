/*
 * ServeStream: A HTTP stream browser/player for Android
 * Copyright 2012 William Seemann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sourceforge.servestream.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import net.sourceforge.servestream.R;
import net.sourceforge.servestream.activity.URLListActivity;
import net.sourceforge.servestream.bean.UriBean;
import net.sourceforge.servestream.dbutils.StreamDatabase;
import net.sourceforge.servestream.transport.TransportFactory;

import org.xmlpull.v1.XmlSerializer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Xml;
import android.widget.Toast;

public class BackupUtils {

	private static final String BACKUP_FILE = "backup.xml";
	
	private static final String ROOT_ELEMENT_TAG_NAME = "backup";
	private static final String BACKUP_ENCODING = "UTF-8";
	
	public static void showBackupDialog(final Context context) {
    	AlertDialog.Builder builder;
    	AlertDialog alertDialog;
    	
    	File backupFile = new File(context.getCacheDir(), BACKUP_FILE);

    	CharSequence[] items;
    	
    	if (backupFile.exists()) {
    		items = new CharSequence[2];
    	    items[0] = context.getString(R.string.list_menu_backup);
    		items[1] = context.getString(R.string.restore);
    	} else {
    		items = new CharSequence[1];
    		items[0] = context.getString(R.string.restore);
    	}

    	builder = new AlertDialog.Builder(context);
    		builder.setItems(items, new DialogInterface.OnClickListener() {
    			public void onClick(DialogInterface dialog, int item) {
    				if (item == 0) {
    					backup(context);
    				} else if (item == 1) {
    					restore(context, BACKUP_FILE);
    					((URLListActivity) context).updateList();
    				}
    			}
    		});
    	alertDialog = builder.create();
    	alertDialog.show();
	}
	
	private static void backup(Context context) {
		BufferedWriter out = null;
		
		StreamDatabase streamdb = new StreamDatabase(context);
		
		String xml = writeXml(streamdb.getUris());
		
		streamdb.close();
		
		File backupDir = context.getCacheDir();
		
		File backupFile = new File(backupDir, BACKUP_FILE);
		try {
			out = new BufferedWriter(new FileWriter(backupFile));
			out.write(xml);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				out.close();
			} catch (IOException e) {
			}
		}
		
		Toast.makeText(context, context.getString(R.string.backup_message), Toast.LENGTH_SHORT).show();
	}
	
	private static void restore(Context context, String fileName) {
		StreamDatabase streamdb = new StreamDatabase(context);
		
		File backupFile = new File(context.getCacheDir(), fileName);
		
		BackupFileParser bfp = new BackupFileParser(backupFile);
		
		List<UriBean> uris = bfp.parse();
		
		for (int i = 0; i < uris.size(); i++) {
			if (TransportFactory.findUri(streamdb, uris.get(i).getUri()) == null) {
				streamdb.saveUri(uris.get(i));
			}
		}
		
		streamdb.close();
		
		Toast.makeText(context, R.string.restore_message, Toast.LENGTH_SHORT).show();
	}
	
	private static String writeXml(List<UriBean> uris) {
	    XmlSerializer serializer = Xml.newSerializer();
	    StringWriter writer = new StringWriter();
	    try {
	        serializer.setOutput(writer);
	        serializer.startDocument(BACKUP_ENCODING, true);
	        serializer.startTag("", ROOT_ELEMENT_TAG_NAME);
	        for (UriBean uriBean: uris) {
	            serializer.startTag("", UriBean.BEAN_NAME);
	        	
        		Set<Entry<String, Object>> selection = uriBean.getValues().valueSet();
	        	
        		Iterator<Entry<String, Object>> i = selection.iterator();
        		
        		while (i.hasNext()) {
        			Entry<String, Object> entry = i.next();
        		
        			String key = entry.getKey();
        			
        			serializer.startTag("", key);
            		if (entry.getValue() != null) {
            			serializer.text(entry.getValue().toString());
            		} else {
            			serializer.text("");
            		}
        			serializer.endTag("", key);
        		}
        		
                serializer.endTag("", UriBean.BEAN_NAME);
	        }
	        serializer.endTag("", ROOT_ELEMENT_TAG_NAME);
	        serializer.endDocument();
	        return writer.toString();
	    } catch (Exception e) {
	        throw new RuntimeException(e);
	    } 
	}
}
