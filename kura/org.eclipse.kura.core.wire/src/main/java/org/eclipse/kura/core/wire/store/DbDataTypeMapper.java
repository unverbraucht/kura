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
package org.eclipse.kura.core.wire.store;

import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.kura.wire.WireDataType;

public class DbDataTypeMapper
{
	private static Map<WireDataType,JdbcType> s_dataTypeMap = new HashMap<WireDataType,JdbcType>();
	static {		
		s_dataTypeMap.put(WireDataType.BYTE,    new JdbcType(Types.TINYINT,  "TINYINT"));
		s_dataTypeMap.put(WireDataType.SHORT,   new JdbcType(Types.SMALLINT, "SMALLINT"));
		s_dataTypeMap.put(WireDataType.INTEGER, new JdbcType(Types.INTEGER,  "INTEGER"));
		s_dataTypeMap.put(WireDataType.LONG,    new JdbcType(Types.BIGINT,   "BIGINT"));
		s_dataTypeMap.put(WireDataType.DOUBLE,  new JdbcType(Types.DOUBLE,   "DOUBLE"));
		s_dataTypeMap.put(WireDataType.BOOLEAN, new JdbcType(Types.BOOLEAN,  "BOOLEAN"));
		s_dataTypeMap.put(WireDataType.RAW,     new JdbcType(Types.BINARY,   "BINARY"));
		s_dataTypeMap.put(WireDataType.STRING,  new JdbcType(Types.VARCHAR,  "VARCHAR(102400)"));
	}

	private static Map<Integer,WireDataType> s_jdbcTypeMap = new HashMap<Integer,WireDataType>();
	static {		
		s_jdbcTypeMap.put(Types.TINYINT,  WireDataType.BYTE);
		s_jdbcTypeMap.put(Types.SMALLINT, WireDataType.SHORT);
		s_jdbcTypeMap.put(Types.INTEGER,  WireDataType.INTEGER);
		s_jdbcTypeMap.put(Types.BIGINT,   WireDataType.LONG);
		s_jdbcTypeMap.put(Types.DOUBLE,   WireDataType.DOUBLE);
		s_jdbcTypeMap.put(Types.BOOLEAN,  WireDataType.BOOLEAN);
		s_jdbcTypeMap.put(Types.BINARY,   WireDataType.RAW);
		s_jdbcTypeMap.put(Types.VARCHAR,  WireDataType.STRING);
	}

	
	public static JdbcType getJdbcType(WireDataType dataType) {
		return s_dataTypeMap.get(dataType);
	}    
    

	public static WireDataType getDataType(int jdbcType) {
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
