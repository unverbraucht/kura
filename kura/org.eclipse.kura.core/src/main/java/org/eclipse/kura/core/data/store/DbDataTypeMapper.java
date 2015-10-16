/**
 * Copyright (c) 2011, 2015 Eurotech and/or its affiliates
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Eurotech
 */
package org.eclipse.kura.core.data.store;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.data.event.DataType;

public class DbDataTypeMapper
{
	private static Map<DataType,JdbcType> s_dataTypeMap = new HashMap<DataType,JdbcType>();
	static {		
		s_dataTypeMap.put(DataType.BYTE,    new JdbcType(Types.TINYINT,  "TINYINT"));
		s_dataTypeMap.put(DataType.SHORT,   new JdbcType(Types.SMALLINT, "SMALLINT"));
		s_dataTypeMap.put(DataType.INTEGER, new JdbcType(Types.INTEGER,  "INTEGER"));
		s_dataTypeMap.put(DataType.LONG,    new JdbcType(Types.BIGINT,   "BIGINT"));
		s_dataTypeMap.put(DataType.DOUBLE,  new JdbcType(Types.DOUBLE,   "DOUBLE"));
		s_dataTypeMap.put(DataType.BOOLEAN, new JdbcType(Types.BOOLEAN,  "BOOLEAN"));
		s_dataTypeMap.put(DataType.RAW,     new JdbcType(Types.BINARY,   "BINARY"));
		s_dataTypeMap.put(DataType.STRING,  new JdbcType(Types.VARCHAR,  "VARCHAR(102400)"));
	}

	private static Map<Integer,DataType> s_jdbcTypeMap = new HashMap<Integer,DataType>();
	static {		
		s_jdbcTypeMap.put(Types.TINYINT,  DataType.BYTE);
		s_jdbcTypeMap.put(Types.SMALLINT, DataType.SHORT);
		s_jdbcTypeMap.put(Types.INTEGER,  DataType.INTEGER);
		s_jdbcTypeMap.put(Types.BIGINT,   DataType.LONG);
		s_jdbcTypeMap.put(Types.DOUBLE,   DataType.DOUBLE);
		s_jdbcTypeMap.put(Types.BOOLEAN,  DataType.BOOLEAN);
		s_jdbcTypeMap.put(Types.BINARY,   DataType.RAW);
		s_jdbcTypeMap.put(Types.VARCHAR,  DataType.STRING);
	}

	
	public static JdbcType getJdbcType(DataType dataType) {
		return s_dataTypeMap.get(dataType);
	}    
    

	public static DataType getDataType(int jdbcType) {
		return s_jdbcTypeMap.get(jdbcType);
	}    

    public static class JdbcType
    {
    	private int    m_type;
    	private String m_typeStr;
    	
    	public JdbcType(int type, String typeStr) {
        	m_type = type;
        	m_typeStr = typeStr;		
    	}

		public int getType() {
			return m_type;
		}

		public String getTypeString() {
			return m_typeStr;
		}
    }
}
