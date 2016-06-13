package com.basho.riakts.jdbc;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Map;

public class ResultSet implements java.sql.ResultSet {
	protected static final int POS_BEFORE_FIRST = -1;
    protected static final int POS_AFTER_LAST = -1;
	protected int direction = FETCH_FORWARD;
	protected int fetchDirection = FETCH_FORWARD;
	
	/** The current row number that is being written to or read from. */
    protected int rowPosition = POS_BEFORE_FIRST;
    
    /** Total number of rows in the ResultSet */
	protected int rowsInResult = 0;
	
	/** Number of columns in the ResultSet */
	protected int columnCount = 0;
	
	/** List of column names imported from the Riak TS QueryResult */
	protected ArrayList<String> columnList;
	
	/** Rows of data copied from Riak TS QueryResult to JDBC ResultSet */
	protected ArrayList<Object[]> rowData;

	/** Whether or not the ResultSet is closed */
	protected boolean closed;

	private Object[] currentRow;
	private Object[] insertRow;
	private boolean inserting = false;
	
	ResultSet() { 
		closed = false;
		rowData = new ArrayList<Object[]>();
		columnList = new ArrayList<String>();
	}
	
	public void close() throws SQLException {
		rowData = null;
		currentRow = null;
		columnList = null;
		rowsInResult = 0;
		columnCount = 0;
		rowPosition = -1;
		closed = true;
	}
	
	public boolean isClosed() throws SQLException {
		return closed;
	}
	
	
	public void moveToInsertRow() throws SQLException {
		// Throw exception if there are no columns or rows in the QueryResult
		if (columnCount == 0 || rowsInResult == 0) throw new SQLException();
		
		// Create a new Object[] to store column values
		insertRow = new Object[columnCount];
		
		// Set inserting to true versus updating an existing row
		inserting = true;
	}
	
	public void insertRow() throws SQLException {
		// Add the new row to the rowData ArrayList<Object[]>
		rowData.add(insertRow);
		
		// Clear insertRow
		insertRow = null;
		
		// Set inserting back to false
		inserting = false;
		
		// Update currentRow to equal our newly added row
		currentRow = rowData.get(rowData.size() - 1);
	}
	
	
	/***
	 * Updates a column based on its index, works for both inserts of new rows
	 * and updates of 
	 * @param columnIndex
	 * @param dataType
	 * @param value
	 * @param length
	 * @throws SQLException 
	 */
	protected void setColumnValue(int columnIndex, Object value) throws SQLException {
		if (columnIndex < 0 || columnIndex > columnCount - 1) throw new SQLException();
		if (inserting) {
			insertRow[columnIndex] = value;
		}
		else {
			currentRow[columnIndex] = value;
		}
	}

	
	// Start - Get Methods that have been implemented for Riak TS
	public Timestamp getTimestamp(int columnIndex) throws SQLException {
		return (Timestamp) currentRow[columnIndex];
	}

	public Timestamp getTimestamp(String columnLabel) throws SQLException {
		return (Timestamp) currentRow[ columnList.indexOf(columnLabel) ];
	}

	public double getDouble(int columnIndex) throws SQLException {
		return Double.parseDouble( (String) currentRow[columnIndex] );
	}
	
	public double getDouble(String columnLabel) throws SQLException {
		return (Double) currentRow[ columnList.indexOf(columnLabel) ];
	}

	public String getString(int columnIndex) throws SQLException {
		return (String) currentRow[columnIndex];
	}
	
	public String getString(String columnLabel) throws SQLException {
		return (String) currentRow[ columnList.indexOf(columnLabel) ];
	}

	public boolean getBoolean(int columnIndex) throws SQLException {
		return (Boolean) currentRow[columnIndex];
	}

	public boolean getBoolean(String columnLabel) throws SQLException {
		return (Boolean) currentRow[ columnList.indexOf(columnLabel) ];
	}
	
	public long getLong(int columnIndex) throws SQLException {
		return (Long) currentRow[columnIndex] ;
	}

	public long getLong(String columnLabel) throws SQLException {
		return (Long) currentRow[ columnList.indexOf(columnLabel) ];
	}
	
	public Object getObject(int columnIndex) throws SQLException {
		return currentRow[columnIndex];
	}

	public Object getObject(String columnLabel) throws SQLException {
		return currentRow[ columnList.indexOf(columnLabel) ];
	}
	// End - Get Methods that have been implemented for Riak TS


	
	public SQLWarning getWarnings() throws SQLException {
		return null;
	}

	public void clearWarnings() throws SQLException { }

	public String getCursorName() throws SQLException {
		return null;
	}

	public ResultSetMetaData getMetaData() throws SQLException {
		return null;
	}

	public int findColumn(String columnLabel) throws SQLException {
		return 0;
	}
	
	
	
	// Start - Row/Cursor position related methods
	/***
	 * Retrieve our Object[] for the specified row
	 * @param i
	 * @throws SQLException
	 */
	private void setCurrentRow(int i) throws SQLException {
		currentRow = rowData.get(i);
	}
	
	public boolean isBeforeFirst() throws SQLException {
		if (rowPosition == -1) {
			return true;
		}
		return false;
	}

	public boolean isAfterLast() throws SQLException {
		if (rowPosition > rowData.size() - 1) {
			return true;
		}
		return false;
	}

	public boolean isFirst() throws SQLException {
		if (rowPosition == 0) {
			return true;
		}
		return false;
	}

	public boolean isLast() throws SQLException {
		if (rowPosition == rowData.size() - 1) {
			return true;
		}
		return false;
	}

	public void beforeFirst() throws SQLException {
		currentRow = null;
		rowPosition = -1;
	}

	public void afterLast() throws SQLException {
		currentRow = null;
		rowPosition = rowData.size() + 1;
	}

	public boolean first() throws SQLException {
		if (rowData.size() > 0) {
			rowPosition = 0;
			setCurrentRow(0);
			return true;
		}
		return false;
	}

	public boolean last() throws SQLException {
		if (rowData.size() > 0) {
			rowPosition = rowData.size() - 1;
			setCurrentRow(rowPosition);
			return true;
		}
		return false;
	}

	public int getRow() throws SQLException {
		return rowPosition;
	}

	public boolean absolute(int row) throws SQLException {
		if (rowData.size() > 0 && row <= rowData.size() && row >= 0) {
			rowPosition = row - 1;
			setCurrentRow(rowPosition);
			return true;
		}
		return false;
	}

	public boolean relative(int rows) throws SQLException {
		if (rowData.size() > 0) {
			int newRowPosition = rowPosition + rows;
			if (newRowPosition > -1 && newRowPosition < rowData.size()) {
				rowPosition = newRowPosition;
				setCurrentRow(rowPosition);
				return true;
			}
		}
		return false;
	}

	public boolean previous() throws SQLException {
		if (rowData.size() > 0 && rowPosition > 0) {
			rowPosition--;
			setCurrentRow(rowPosition);
			return true;
		}
		return false;
	}
		
	public boolean next() throws SQLException {
		if (rowData.size() > 0 && rowPosition < rowData.size() - 1) {
			rowPosition++;
			setCurrentRow(rowPosition);
			return true;
		}
		return false;
	}
	// End - Row/Cursor position related methods
	


	public void setFetchDirection(int direction) throws SQLException { }

	public int getFetchDirection() throws SQLException {
		return 0;
	}

	public void setFetchSize(int rows) throws SQLException { }

	public int getFetchSize() throws SQLException {
		return 0;
	}

	public int getType() throws SQLException {
		return TYPE_FORWARD_ONLY;
	}

	public int getConcurrency() throws SQLException {
		return CONCUR_READ_ONLY;
	}

	public boolean rowUpdated() throws SQLException {
		return false;
	}

	public boolean rowInserted() throws SQLException {
		return false;
	}

	public boolean rowDeleted() throws SQLException {
		return false;
	}
	
	
	// Update Column Methods
	public void updateNull(int columnIndex) throws SQLException {
		setColumnValue(columnIndex, null);
	}
	
	public void updateNull(String columnLabel) throws SQLException {
		setColumnValue(columnList.indexOf(columnLabel), null);
	}

	public void updateBoolean(int columnIndex, boolean x) throws SQLException {
		setColumnValue(columnIndex, x);
	}
	
	public void updateBoolean(String columnLabel, boolean x) throws SQLException {
		setColumnValue(columnList.indexOf(columnLabel), x);
	}

	public void updateLong(int columnIndex, long x) throws SQLException {
		setColumnValue(columnIndex, x);
	}
	
	public void updateLong(String columnLabel, long x) throws SQLException {
		setColumnValue(columnList.indexOf(columnLabel), x);
	}

	public void updateDouble(int columnIndex, double x) throws SQLException {
		setColumnValue(columnIndex, x);
	}
	
	public void updateDouble(String columnLabel, double x) throws SQLException {
		setColumnValue(columnList.indexOf(columnLabel), x);
	}

	public void updateString(int columnIndex, String x) throws SQLException {
		setColumnValue(columnIndex, x);
	}

	public void updateString(String columnLabel, String x) throws SQLException {
		setColumnValue(columnList.indexOf(columnLabel), x);
	}
	
	public void updateDate(int columnIndex, Date x) throws SQLException {
		setColumnValue(columnIndex, x);
	}
	
	public void updateDate(String columnLabel, Date x) throws SQLException {
		setColumnValue(columnList.indexOf(columnLabel), x);
	}
	
	public void updateTimestamp(String columnLabel, Timestamp x) throws SQLException { 
		setColumnValue(columnList.indexOf(columnLabel), x);
	}
	
	public void updateTimestamp(int columnIndex, Timestamp x) throws SQLException { 
		setColumnValue(columnIndex, x);
	}


	
	// Update methods not implement for Riak TS
	
	/***
	 * Method not implemented - no value returned
	 */
	public void updateFloat(int columnIndex, float x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBytes(int columnIndex, byte[] x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateByte(int columnIndex, byte x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateShort(int columnIndex, short x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateInt(int columnIndex, int x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateTime(int columnIndex, Time x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateAsciiStream(int columnIndex, InputStream x, int length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBinaryStream(int columnIndex, InputStream x, int length) throws SQLException {  }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateCharacterStream(int columnIndex, Reader x, int length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateObject(int columnIndex, Object x, int scaleOrLength) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateObject(int columnIndex, Object x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateFloat(String columnLabel, float x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBigDecimal(String columnLabel, BigDecimal x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateTime(String columnLabel, Time x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateAsciiStream(String columnLabel, InputStream x, int length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBinaryStream(String columnLabel, InputStream x, int length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateCharacterStream(String columnLabel, Reader reader, int length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateObject(String columnLabel, Object x, int scaleOrLength) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateObject(String columnLabel, Object x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateByte(String columnLabel, byte x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateShort(String columnLabel, short x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateInt(String columnLabel, int x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBytes(String columnLabel, byte[] x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateRef(int columnIndex, Ref x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateRef(String columnLabel, Ref x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBlob(int columnIndex, Blob x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBlob(String columnLabel, Blob x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateClob(int columnIndex, Clob x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateClob(String columnLabel, Clob x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateArray(int columnIndex, Array x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateArray(String columnLabel, Array x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateNString(int columnIndex, String nString) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateNString(String columnLabel, String nString) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateNClob(int columnIndex, NClob nClob) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateNClob(String columnLabel, NClob nClob) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateClob(int columnIndex, Reader reader, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateClob(String columnLabel, Reader reader, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateCharacterStream(int columnIndex, Reader x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateClob(int columnIndex, Reader reader) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateClob(String columnLabel, Reader reader) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateNClob(int columnIndex, Reader reader) throws SQLException { }
	/***
	 * Method not implemented - no value returned
	 */
	public void updateNClob(String columnLabel, Reader reader) throws SQLException { }

	
	/***
	 * Method not implemented - no value returned
	 */
	public void updateRow() throws SQLException {
		
	}

	public void deleteRow() throws SQLException {
		rowData.remove(rowPosition);
	}

	/***
	 * Method not implemented - no value returned
	 */
	public void refreshRow() throws SQLException {
		
	}

	/***
	 * Method not implemented - no value returned
	 */
	public void cancelRowUpdates() throws SQLException {
		
	}

	/***
	 * Method not implemented - no value returned
	 */
	public void moveToCurrentRow() throws SQLException {
		
	}

	/***
	 * Method not implemented - null value returned
	 */
	public Statement getStatement() throws SQLException {
		return null;
	}
	
	
	

	// Get Methods not implemented for Riak TS
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Date getDate(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Date getDate(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Date getDate(int columnIndex, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Date getDate(String columnLabel, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Object getObject(int columnIndex, Map<String, Class<?>> map) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Object getObject(String columnLabel, Map<String, Class<?>> map) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Ref getRef(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Ref getRef(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public byte getByte(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public short getShort(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public int getInt(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public float getFloat(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public byte[] getBytes(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Time getTime(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Blob getBlob(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Clob getClob(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Array getArray(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Blob getBlob(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Clob getClob(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Array getArray(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Time getTime(int columnIndex, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Time getTime(String columnLabel, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Timestamp getTimestamp(String columnLabel, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public URL getURL(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public URL getURL(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public NClob getNClob(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public NClob getNClob(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public SQLXML getSQLXML(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public SQLXML getSQLXML(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public String getNString(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public String getNString(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Reader getNCharacterStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Reader getNCharacterStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public <T> T getObject(int columnIndex, Class<T> type) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public <T> T getObject(String columnLabel, Class<T> type) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public InputStream getAsciiStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public InputStream getUnicodeStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public InputStream getBinaryStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public InputStream getAsciiStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public InputStream getUnicodeStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public InputStream getBinaryStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public byte[] getBytes(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Reader getCharacterStream(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Reader getCharacterStream(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public byte getByte(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public short getShort(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public int getInt(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public float getFloat(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public BigDecimal getBigDecimal(String columnLabel, int scale) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Time getTime(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public BigDecimal getBigDecimal(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	
	
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public RowId getRowId(int columnIndex) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public RowId getRowId(String columnLabel) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - no value returned
	 */
	public void updateRowId(int columnIndex, RowId x) throws SQLException { }
	/***
	 * Not implemented - no value returned
	 */
	public void updateRowId(String columnLabel, RowId x) throws SQLException { }
	

	public int getHoldability() throws SQLException {
		return 0;
	}

	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public <T> T unwrap(Class<T> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}
	
	/***
	 * Not implemented - throws UnsupportedOperationException
	 */
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		throw new UnsupportedOperationException();
	}


	public boolean wasNull() throws SQLException {
		return false;
	}	

}
